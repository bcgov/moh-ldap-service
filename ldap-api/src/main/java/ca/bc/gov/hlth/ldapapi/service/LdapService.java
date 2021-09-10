package ca.bc.gov.hlth.ldapapi.service;

import ca.bc.gov.hlth.ldapapi.model.User;
import ca.bc.gov.hlth.ldapapi.model.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LdapService {

    private final Logger webClientLogger = LoggerFactory.getLogger(LdapService.class);
    private final Properties ldapProperties;

    private static final String LDAP_CONST_UNLOCKED = "unlocked";
    private static final String LDAP_CONST_LOCKED = "locked";
    private static final String LDAP_CONST_ACCOUNT_LOCKED_ATTRIBUTE = "acctlockedflag";
    private static final String LDAP_ATTR_ACCT_LOCKED_BY = "acctlockedby";
    private static final String LDAP_ATTR_ACCT_LOCKED_REASON = "acctlockedreason";
    private static final String LDAP_ATTR_PASSWORD_CHANGE_DATE = "passwordchangedate";
    private static final String LDAP_ATTR_PASSLIFESPAN = "passlifespan";
    private static final String LDAP_SEARCH_BASE = "o=hnet,st=bc,c=ca";

    @Value("${ldap.attempt.timeout}")
    protected int attemptTimeout;

    protected final ConcurrentHashMap<String, LoginAttempts> loginAttemptsMap = new ConcurrentHashMap<>();

    public LdapService(Properties ldapProperties) {
        this.ldapProperties = ldapProperties;
    }

    public User authenticate(UserCredentials userCredentials) {
        SearchResult userInfo = searchUser(userCredentials.getUserName());
        if (userInfo == null) {
            return null;
        }

        boolean userUnlocked = checkUserLocked(userInfo.getAttributes());

        boolean validCredentials = false;
        long lockoutTimeInHours = 0;
        int remainingAttempts = 3;

        if (userUnlocked) {
            AuthenticationResult authenticationResult = authenticateUser(userInfo.getName(), userCredentials.getPassword());
            validCredentials = authenticationResult.isAuthenticated;
            userUnlocked = !authenticationResult.isLocked;

            // Get nbLoginAttempts and lockoutTimeOut after authenticateUser
            if (!validCredentials) {
                LoginAttempts loginAttemptsForUser = loginAttemptsMap.get(userInfo.getName());
                if (loginAttemptsForUser != null) {
                    remainingAttempts = 3 - loginAttemptsForUser.getAttempts();
                    lockoutTimeInHours = ChronoUnit.HOURS.between(loginAttemptsForUser.getLastAttempt(), LocalDateTime.now());
                }
            }
        }

        return createReturnMessage(userInfo.getName(), validCredentials, userUnlocked, lockoutTimeInHours, remainingAttempts, userInfo.getAttributes());
    }

    private SearchResult searchUser(String username) {
        try {
            DirContext ctx = new InitialDirContext(ldapProperties);
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(2);
            constraints.setCountLimit(300L); // TODO filter out names that could cause a wildcard search
            constraints.setReturningObjFlag(false);
            NamingEnumeration<SearchResult> results = ctx.search(LDAP_SEARCH_BASE, "uid=" + username, constraints);
            ctx.close();
            if (results.hasMore()) {
                return results.next();
            } else {
                return null;
            }


        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkUserLocked(Attributes attributes) {
        Attribute acctLockedFlag = attributes.get(LDAP_CONST_ACCOUNT_LOCKED_ATTRIBUTE);
        boolean unlocked = true;

        if (acctLockedFlag != null) {
            try {
                // If the attribute exists the account is locked in any case except for a value of "unlocked"
                unlocked = LDAP_CONST_UNLOCKED.equalsIgnoreCase(acctLockedFlag.get().toString());
            } catch (NamingException e) { // TODO Naming exception should return a 500, NoSuchElementException can be caught
                e.printStackTrace();
            }
        }
        return unlocked;
    }

    private AuthenticationResult authenticateUser(String userInfoName, String password) {

        boolean userAuthenticated = false;
        boolean accountLocked = false;

        Properties userLdapProperties = (Properties) ldapProperties.clone();
        userLdapProperties.put("java.naming.security.principal", userInfoName + "," + LDAP_SEARCH_BASE);
        userLdapProperties.put("java.naming.security.credentials", password);
        userLdapProperties.put("com.sun.jndi.ldap.connect.pool", "false");

        try {
            DirContext userContext = new InitialDirContext(userLdapProperties);
            userAuthenticated = true;
            userContext.close(); // close can also throw an exception
        } catch (NamingException e) {
            if (e instanceof AuthenticationException) {
                accountLocked = updateUserFailedLoginAttempts(userInfoName);
                webClientLogger.info("Failed authentication for user: " + userInfoName);
            } else {
                e.printStackTrace(); // TODO we should return a 500 here
            }
        }

        return new AuthenticationResult(userAuthenticated, accountLocked);
    }

    protected boolean updateUserFailedLoginAttempts(String userInfoName) {
        LoginAttempts loginAttemptsForUser = loginAttemptsMap.get(userInfoName);
        // No entry: add entry, set attempts=1, set timestamp=now
        if (loginAttemptsForUser == null) {
            LoginAttempts newLoginAttempts = new LoginAttempts(1, LocalDateTime.now());
            loginAttemptsMap.put(userInfoName, newLoginAttempts);
            return false;
        }

        long hoursSinceLastAttempt = ChronoUnit.HOURS.between(loginAttemptsForUser.getLastAttempt(), LocalDateTime.now());
        int currentAttempts = loginAttemptsForUser.getAttempts();
        // Entry exists AND timestamp >1hr: set attempts=1, timestamp=now
        if (hoursSinceLastAttempt > attemptTimeout) { // reset the nb of attempts if the attemptsTimeout has passed
            loginAttemptsForUser.setAttempts(1);
            loginAttemptsForUser.setLastAttempt(LocalDateTime.now());

        } else if (currentAttempts < 2) { // Entry Exists, timestamp<1hr, attempts<2: set attempts+1, timestamp=now
            loginAttemptsForUser.setAttempts(currentAttempts + 1);
            loginAttemptsForUser.setLastAttempt(LocalDateTime.now());

        } else { // Entry Exists, timestamp<1hr, attempts >=3
            lockUserAccount(userInfoName + "," + LDAP_SEARCH_BASE);
            loginAttemptsMap.remove(userInfoName);
            return true;
        }
        return false;
    }

    protected User createReturnMessage(String userName, boolean validCredentials, boolean userUnlocked,
                                       long lockoutTimeInHours, int remainingAttempts,
                                       Attributes attributes) {

        User userToReturn = new User();
        userToReturn.setUsername(userName);
        userToReturn.setAuthenticated(validCredentials);
        userToReturn.setUnlocked(userUnlocked);

        // Role and Expiry information is only relevant if the user is authenticated and unlocked
        if (validCredentials && userUnlocked) {

            Attribute gisUserRoleAttribute = attributes.get("gisuserrole");
            if (gisUserRoleAttribute != null) {
                try {
                    userToReturn.setGisuserrole((String) gisUserRoleAttribute.get());
                } catch (NamingException e) {
                    e.printStackTrace(); // TODO we should return a 500 here
                }
            }
        } else if (!validCredentials) {
            userToReturn.setLockoutTimeInHours(lockoutTimeInHours);
            userToReturn.setRemainingAttempts(remainingAttempts);
        }

        return userToReturn;
    }
        
    protected void lockUserAccount(String userInfoName) {
        try {
            DirContext ctx = new InitialDirContext(ldapProperties);

            ModificationItem lockedItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute(LDAP_CONST_ACCOUNT_LOCKED_ATTRIBUTE, LDAP_CONST_LOCKED));
            ModificationItem lockedReasonItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute(LDAP_ATTR_ACCT_LOCKED_REASON, "User exceeded maximum login attempts"));
            ModificationItem lockedByItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute(LDAP_ATTR_ACCT_LOCKED_BY, "KeycloakEnrollment"));

            ModificationItem[] changeItems = {lockedItem, lockedReasonItem, lockedByItem};

            ctx.modifyAttributes(userInfoName, changeItems);
            webClientLogger.info("User locked: " + userInfoName);

        } catch (NamingException e) {
            webClientLogger.info("Failed to lock user: " + userInfoName);
            e.printStackTrace();
        }
    }

    private static class AuthenticationResult {
        boolean isAuthenticated;
        boolean isLocked;

        public AuthenticationResult(boolean isAuthenticated, boolean locked) {
            this.isAuthenticated = isAuthenticated;
            this.isLocked = locked;
        }
    }

}

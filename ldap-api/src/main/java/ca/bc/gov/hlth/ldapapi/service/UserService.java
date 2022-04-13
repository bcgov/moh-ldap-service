package ca.bc.gov.hlth.ldapapi.service;

import ca.bc.gov.hlth.ldapapi.model.UserCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final LdapService ldapService;

    @Value("${ldap.attempt.timeout}")
    int attemptTimeout;

    @Value("${roleAttributeNames}")
    private String roles;

    final ConcurrentHashMap<String, LoginAttempts> loginAttemptsMap = new ConcurrentHashMap<>();

    public UserService(LdapService ldapService) {
        this.ldapService = ldapService;
    }

    public Object authenticate(UserCredentials userCredentials) throws NamingException {
        SearchResult userInfo = ldapService.searchUser(userCredentials.getUserName());
        if (userInfo == null) {
            return null;
        }

        boolean userUnlocked = ldapService.checkUserLocked(userInfo.getAttributes());

        boolean validCredentials = false;
        long lockoutTimeInHours = 0;
        int remainingAttempts = 3;

        if (userUnlocked) {
            validCredentials = ldapService.authenticateUser(userInfo.getName(), userCredentials.getPassword());

            if (!validCredentials) {
                userUnlocked = !updateUserFailedLoginAttempts(userInfo.getName());
            }

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

    boolean updateUserFailedLoginAttempts(String userInfoName) throws NamingException {
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
            ldapService.lockUserAccount(userInfoName);
            loginAttemptsMap.remove(userInfoName);
            return true;
        }
        return false;
    }

    private Map<String, Object> createReturnMessage(String userName, boolean validCredentials, boolean userUnlocked,
                                                    long lockoutTimeInHours, int remainingAttempts,
                                                    Attributes attributes) throws NamingException {

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userName", userName);
        userMap.put("authenticated", validCredentials);
        userMap.put("unlocked", userUnlocked);

        if (validCredentials && userUnlocked) {
            Map<String, String> org = new HashMap<>();
            org.put("id", "00002855");
            org.put("name", "PRS BCMOH - Registry Administrator");
            userMap.put("org_details", org);
            for (String role : roles.split(",")) {
                Attribute userRoleAttribute = attributes.get(role);
                if (userRoleAttribute != null) {
                    userMap.put(role, userRoleAttribute.get());
                }
            }
        } else if (!validCredentials) {
            userMap.put("lockoutTimeInHours", lockoutTimeInHours);
            userMap.put("remainingAttempts", remainingAttempts);
        }

        return userMap;
    }

    private static class LoginAttempts {
        private int attempts;
        private LocalDateTime lastAttempt;

        LoginAttempts(int attempts, LocalDateTime lastAttempt) {
            this.attempts = attempts;
            this.lastAttempt = lastAttempt;
        }

        int getAttempts() {
            return attempts;
        }

        void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        LocalDateTime getLastAttempt() {
            return lastAttempt;
        }

        void setLastAttempt(LocalDateTime lastAttempt) {
            this.lastAttempt = lastAttempt;
        }
    }

}

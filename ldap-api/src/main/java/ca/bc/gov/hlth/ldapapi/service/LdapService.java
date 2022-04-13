package ca.bc.gov.hlth.ldapapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Properties;

@Service
public class LdapService {

    private final Logger logger = LoggerFactory.getLogger(LdapService.class);

    private static final String LDAP_CONST_UNLOCKED = "unlocked";
    private static final String LDAP_CONST_LOCKED = "locked";
    private static final String LDAP_CONST_ACCOUNT_LOCKED_ATTRIBUTE = "acctlockedflag";
    private static final String LDAP_ATTR_ACCT_LOCKED_BY = "acctlockedby";
    private static final String LDAP_ATTR_ACCT_LOCKED_REASON = "acctlockedreason";
    private static final String LDAP_SEARCH_BASE = "o=hnet,st=bc,c=ca";

    private Properties ldapProperties;

    public LdapService(Properties ldapProperties) {
        this.ldapProperties = ldapProperties;
    }

    SearchResult searchUser(String username) throws NamingException {
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
    }

    boolean authenticateUser(String userInfoName, String password) throws NamingException {

        Properties userLdapProperties = (Properties) ldapProperties.clone();
        userLdapProperties.put("java.naming.security.principal", userInfoName + "," + LDAP_SEARCH_BASE);
        userLdapProperties.put("java.naming.security.credentials", password);
        userLdapProperties.put("com.sun.jndi.ldap.connect.pool", "false");

        try {
            DirContext userContext = new InitialDirContext(userLdapProperties);
            userContext.close(); // close can also throw an exception
        } catch (NamingException e) {
            if (e instanceof AuthenticationException) {
                logger.info("Failed authentication for user: " + userInfoName);
                return false;
            } else {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    void lockUserAccount(String userInfoName) throws NamingException {
        userInfoName += "," + LDAP_SEARCH_BASE;
        DirContext ctx = new InitialDirContext(ldapProperties);

        ModificationItem lockedItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute(LDAP_CONST_ACCOUNT_LOCKED_ATTRIBUTE, LDAP_CONST_LOCKED));
        ModificationItem lockedReasonItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute(LDAP_ATTR_ACCT_LOCKED_REASON, "User exceeded maximum login attempts"));
        ModificationItem lockedByItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute(LDAP_ATTR_ACCT_LOCKED_BY, "KeycloakEnrollment"));

        ModificationItem[] changeItems = {lockedItem, lockedReasonItem, lockedByItem};

        ctx.modifyAttributes(userInfoName, changeItems);
        logger.info("User locked: " + userInfoName);
    }

    boolean checkUserLocked(Attributes attributes) throws NamingException {
        Attribute acctLockedFlag = attributes.get(LDAP_CONST_ACCOUNT_LOCKED_ATTRIBUTE);
        boolean unlocked = true;

        if (acctLockedFlag != null) {
            // If the attribute exists the account is locked in any case except for a value of "unlocked"
            unlocked = LDAP_CONST_UNLOCKED.equalsIgnoreCase(acctLockedFlag.get().toString());
        }
        return unlocked;
    }
}

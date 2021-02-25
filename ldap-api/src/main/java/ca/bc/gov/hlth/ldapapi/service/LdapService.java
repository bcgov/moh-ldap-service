package ca.bc.gov.hlth.ldapapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class LdapService {

    private final Logger webClientLogger = LoggerFactory.getLogger(LdapService.class);
    private final Properties ldapProperties;

    public LdapService(Properties ldapProperties) {
        this.ldapProperties = ldapProperties;
    }

    public Map<String, String> authenticate(String username, String password) {
        SearchResult userInfo = searchUser(username);
        boolean validCredentials = authenticateUser(userInfo.getName(), password);

        return createReturnMessage(userInfo.getName(), validCredentials, userInfo.getAttributes());
    }

    private SearchResult searchUser(String username) {
        try {
            DirContext ctx = new InitialDirContext(ldapProperties);
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(2);
            constraints.setCountLimit(300L); // TODO filter out names that could cause a wildcard search
            constraints.setReturningObjFlag(false);
            NamingEnumeration<SearchResult> results = ctx.search("o=hnet,st=bc,c=ca", "uid=" + username, constraints);
            ctx.close();

            return results.next();

        } catch (NamingException e) {
            e.printStackTrace(); // TODO separate responses based on exceptions
        }
        return null;
    }

    private boolean authenticateUser(String userInfoName, String password) {

        boolean userAuthenticated = false;

        Properties userLdapProperties = (Properties) ldapProperties.clone();
        userLdapProperties.put("java.naming.security.principal", userInfoName + ",o=hnet,st=bc,c=ca");
        userLdapProperties.put("java.naming.security.credentials", password);
        userLdapProperties.put("com.sun.jndi.ldap.connect.pool", "false");

        try {
            DirContext userContext = new InitialDirContext(userLdapProperties);
            userAuthenticated = true;
            userContext.close(); // close can also throw an exception
        } catch (NamingException e) {
            if (e instanceof AuthenticationException) {
                webClientLogger.info("Failed authentication for user: " + userInfoName);
            } else {
                e.printStackTrace(); // TODO separate authentication exception from other exceptions
            }
        }

        return userAuthenticated;
    }

    protected Map<String, String> createReturnMessage(String userName, boolean validCredentials, Attributes attributes) {

        Map<String, String> returnMessageMap = new HashMap<>();
        returnMessageMap.put("Username", userName);
        returnMessageMap.put("Authenticated", Boolean.toString(validCredentials));
        if (validCredentials) {
            Attribute gisUserRoleAttribute = attributes.get("gisuserrole");
            if (gisUserRoleAttribute != null) {
                try {
                    returnMessageMap.put(gisUserRoleAttribute.getID(), (String) gisUserRoleAttribute.get());
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }
        }

        return returnMessageMap;
    }

}

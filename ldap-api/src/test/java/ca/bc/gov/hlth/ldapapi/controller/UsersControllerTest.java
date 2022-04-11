package ca.bc.gov.hlth.ldapapi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"ConstantConditions", "rawtypes"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersControllerTest {

    @LocalServerPort
    private int port;

    private String urlUnderTest;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    Properties ldapProperties;

    @Value("${LDAP_API_INTEGRATION_TEST_PASSWORD}")
    String PASSWORD;

    public static final String USERNAME = "1-primehcimintegrationtest";

    private static boolean runOnce = true;

    @BeforeEach
    public void init() throws InterruptedException, NamingException {
        urlUnderTest = "http://localhost:" + port + "/users";
        if (runOnce) {
            SearchResult searchResult = searchUser(USERNAME);
            unlockUserAccount(searchResult.getName() + "," + LDAP_SEARCH_BASE);
            runOnce = false;
        }
    }

    @Test
    public void postUser_validPassword_isAuthenticated() {
        Map<String, String> data = new HashMap<>();
        data.put("userName", USERNAME);
        data.put("password", PASSWORD);
        ResponseEntity<Map> response = restTemplate.postForEntity(urlUnderTest, data, Map.class);
        assertAll(
                () -> assertTrue((Boolean) response.getBody().get("authenticated")),
                () -> assertTrue((Boolean) response.getBody().get("unlocked")),
                () -> assertTrue(response.getBody().containsKey("userName")),
                () -> assertTrue(response.getBody().containsKey("hcmuserrole"))
        );
    }

    @Test
    public void postUser_invalidPassword_isNotAuthenticated() {
        Map<String, String> data = new HashMap<>();
        data.put("userName", USERNAME);
        data.put("password", "not_a_real_password");
        ResponseEntity<Map> response = restTemplate.postForEntity(urlUnderTest, data, Map.class);
        assertAll(
                () -> assertFalse((Boolean) response.getBody().get("authenticated")),
                () -> assertTrue((Boolean) response.getBody().get("unlocked")),
                () -> assertTrue(response.getBody().containsKey("userName")),
                () -> assertFalse(response.getBody().containsKey("hcmuserrole")),
                // Other tests may be using 1-primehcimintegrationtest, so we don't know the exact count,
                () -> assertTrue(((Integer) response.getBody().get("remainingAttempts")) < 3)
        );
    }

    @Test
    public void postUser_unknownUser_emptyBody() {
        Map<String, String> data = new HashMap<>();
        data.put("userName", "not_a_real_user");
        data.put("password", "not_a_real_password");
        ResponseEntity<Map> response = restTemplate.postForEntity(urlUnderTest, data, Map.class);
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void postUser_lockedUser_isLocked() {
        Map<String, String> data = new HashMap<>();
        data.put("userName", "2-primehcimintegrationtest");
        // This is a valid password, but "authenticated" will still be false.
        data.put("password", PASSWORD);
        ResponseEntity<Map> response = restTemplate.postForEntity(urlUnderTest, data, Map.class);
        assertAll(
                () -> assertFalse((Boolean) response.getBody().get("authenticated")),
                () -> assertFalse((Boolean) response.getBody().get("unlocked")),
                () -> assertTrue(response.getBody().containsKey("userName")),
                () -> assertFalse(response.getBody().containsKey("hcmuserrole")),
                () -> assertEquals(3, response.getBody().get("remainingAttempts"))
        );
    }

    @Test
    public void postUser_invalidPasswordThreeTimes_lockedAccount() throws NamingException {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("userName", USERNAME);
            data.put("password", "bad_password");

            ResponseEntity<Map> response = restTemplate.postForEntity(urlUnderTest, data, Map.class);
            assertAll(
                    () -> assertFalse((Boolean) response.getBody().get("authenticated")),
                    () -> assertTrue((Boolean) response.getBody().get("unlocked")),
                    () -> assertEquals(2, response.getBody().get("remainingAttempts"))
            );

            ResponseEntity<Map> response2 = restTemplate.postForEntity(urlUnderTest, data, Map.class);
            assertAll(
                    () -> assertFalse((Boolean) response2.getBody().get("authenticated")),
                    () -> assertTrue((Boolean) response2.getBody().get("unlocked")),
                    () -> assertEquals(1, response2.getBody().get("remainingAttempts"))
            );

            ResponseEntity<Map> response3 = restTemplate.postForEntity(urlUnderTest, data, Map.class);
            assertAll(
                    () -> assertFalse((Boolean) response3.getBody().get("authenticated")),
                    () -> assertFalse((Boolean) response3.getBody().get("unlocked")),
                    () -> assertEquals(3, response3.getBody().get("remainingAttempts"))
            );
        } finally {
            SearchResult searchResult = searchUser(USERNAME);
            unlockUserAccount(searchResult.getName() + "," + LDAP_SEARCH_BASE);
        }
    }

    private static final String LDAP_CONST_UNLOCKED = "unlocked";
    private static final String LDAP_CONST_LOCKED = "locked";
    private static final String LDAP_CONST_ACCOUNT_LOCKED_ATTRIBUTE = "acctlockedflag";
    private static final String LDAP_ATTR_ACCT_LOCKED_BY = "acctlockedby";
    private static final String LDAP_ATTR_ACCT_LOCKED_REASON = "acctlockedreason";
    private static final String LDAP_SEARCH_BASE = "o=hnet,st=bc,c=ca";

    @SuppressWarnings("unused")
    void lockUserAccount(String userInfoName) throws NamingException {
        DirContext ctx = new InitialDirContext(ldapProperties);

        ModificationItem lockedItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute(LDAP_CONST_ACCOUNT_LOCKED_ATTRIBUTE, LDAP_CONST_LOCKED));
        ModificationItem lockedReasonItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute(LDAP_ATTR_ACCT_LOCKED_REASON, "User exceeded maximum login attempts"));
        ModificationItem lockedByItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute(LDAP_ATTR_ACCT_LOCKED_BY, "KeycloakEnrollment"));

        ModificationItem[] changeItems = {lockedItem, lockedReasonItem, lockedByItem};

        ctx.modifyAttributes(userInfoName, changeItems);
    }

    @SuppressWarnings("unused")
    void unlockUserAccount(String userInfoName) throws NamingException {
        DirContext ctx = new InitialDirContext(ldapProperties);

        ModificationItem lockedItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute(LDAP_CONST_ACCOUNT_LOCKED_ATTRIBUTE, LDAP_CONST_UNLOCKED));
        ModificationItem lockedReasonItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute(LDAP_ATTR_ACCT_LOCKED_REASON, ""));
        ModificationItem lockedByItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute(LDAP_ATTR_ACCT_LOCKED_BY, ""));

        ModificationItem[] changeItems = {lockedItem, lockedReasonItem, lockedByItem};

        ctx.modifyAttributes(userInfoName, changeItems);
    }

    @SuppressWarnings("SameParameterValue")
    private SearchResult searchUser(String username) throws NamingException {
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

}
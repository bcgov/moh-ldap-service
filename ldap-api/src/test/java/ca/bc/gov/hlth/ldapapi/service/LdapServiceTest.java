package ca.bc.gov.hlth.ldapapi.service;

import org.junit.jupiter.api.Test;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class LdapServiceTest {

    private String userName = "TestUserID";
    private Attributes attributesWithRole = new BasicAttributes("gisuserrole", "GISUSER");
    private Attributes attributesWithoutRole = new BasicAttributes();

    private LdapService ldapService = new LdapService(new Properties());

    @Test
    public void testCreateReturnMessage_authenticatedFalse() {
        Map<String, String> returnMessageMap = ldapService.createReturnMessage(userName, false, attributesWithRole);
        assertEquals(userName, returnMessageMap.get("Username"));
        assertEquals("false", returnMessageMap.get("Authenticated"));
        assertNull(returnMessageMap.get("gisuserrole"));
    }

    @Test
    public void testCreateReturnMessage_authenticatedTrue_RoleFalse() {
        Map<String, String> returnMessageMap = ldapService.createReturnMessage(userName, true, attributesWithoutRole);
        assertEquals(userName, returnMessageMap.get("Username"));
        assertEquals("true", returnMessageMap.get("Authenticated"));
        assertNull(returnMessageMap.get("gisuserrole"));
    }

    @Test
    public void testCreateReturnMessage_authenticatedTrue_RoleTrue() {
        Map<String, String> returnMessageMap = ldapService.createReturnMessage(userName, true, attributesWithRole);
        assertEquals(userName, returnMessageMap.get("Username"));
        assertEquals("true", returnMessageMap.get("Authenticated"));
        assertEquals("GISUSER", returnMessageMap.get("gisuserrole"));
    }
}

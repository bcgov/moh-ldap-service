package ca.bc.gov.hlth.ldapapi.service;

import ca.bc.gov.hlth.ldapapi.model.User;
import org.junit.jupiter.api.Test;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class LdapServiceTest {

    private String userName = "TestUserID";
    private Attributes attributesWithRole = new BasicAttributes("gisuserrole", "GISUSER");
    private Attributes attributesWithoutRole = new BasicAttributes();

    private LdapService ldapService = new LdapService(new Properties());

    @Test
    public void testCreateReturnMessage_authenticatedFalse() {
        User returnedUser = ldapService.createReturnMessage(userName, false, true, attributesWithRole);
        assertEquals(userName, returnedUser.getUsername());
        assertEquals(false, returnedUser.isAuthenticated());
        assertEquals(true, returnedUser.isUnlocked());
        assertNull(returnedUser.getGisuserrole());
    }

    @Test
    public void testCreateReturnMessage_authenticatedTrue_userUnlockedFalse() {
        User returnedUser = ldapService.createReturnMessage(userName, true, false, attributesWithRole);
        assertEquals(userName, returnedUser.getUsername());
        assertEquals(true, returnedUser.isAuthenticated());
        assertEquals(false, returnedUser.isUnlocked());
        assertNull(returnedUser.getGisuserrole());
    }

    @Test
    public void testCreateReturnMessage_authenticatedTrue_RoleFalse() {
        User returnedUser = ldapService.createReturnMessage(userName, true, true, attributesWithoutRole);
        assertEquals(userName, returnedUser.getUsername());
        assertEquals(true, returnedUser.isAuthenticated());
        assertEquals(true, returnedUser.isUnlocked());
        assertNull(returnedUser.getGisuserrole());
    }

    @Test
    public void testCreateReturnMessage_authenticatedTrue_RoleTrue() {
        User returnedUser = ldapService.createReturnMessage(userName, true, true, attributesWithRole);
        assertEquals(userName, returnedUser.getUsername());
        assertEquals(true, returnedUser.isAuthenticated());
        assertEquals(true, returnedUser.isUnlocked());
        assertEquals("GISUSER", returnedUser.getGisuserrole());
    }
}

package ca.bc.gov.hlth.ldapapi.service;

import ca.bc.gov.hlth.ldapapi.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class LdapServiceTest {

    private final String userName = "TestUserID";
    private final Attributes attributesWithRole = new BasicAttributes("gisuserrole", "GISUSER");
    private final Attributes attributesWithoutRole = new BasicAttributes();

    private static final LdapService ldapService = new LdapService(new Properties());

    @BeforeAll
    public static void initialSetup() {
        ldapService.attemptTimeout = 1;
    }

    @Test
    public void testCreateReturnMessage_authenticatedFalse() {
        User returnedUser = ldapService.createReturnMessage(userName, false, true, true, attributesWithRole);
        assertEquals(userName, returnedUser.getUsername());
        assertFalse(returnedUser.isAuthenticated());
        assertFalse(returnedUser.isPasswordExpired());
        assertNull(returnedUser.getGisuserrole());
    }

    @Test
    public void testCreateReturnMessage_authenticatedTrue_userUnlockedFalse() {
        User returnedUser = ldapService.createReturnMessage(userName, true, false, true, attributesWithRole);
        assertEquals(userName, returnedUser.getUsername());
        assertTrue(returnedUser.isAuthenticated());
        assertFalse(returnedUser.isUnlocked());
        assertFalse(returnedUser.isPasswordExpired());
        assertNull(returnedUser.getGisuserrole());
    }

    @Test
    public void testCreateReturnMessage_authenticatedTrue_RoleFalse() {
        User returnedUser = ldapService.createReturnMessage(userName, true, true, false, attributesWithoutRole);
        assertEquals(userName, returnedUser.getUsername());
        assertTrue(returnedUser.isAuthenticated());
        assertTrue(returnedUser.isUnlocked());
        assertFalse(returnedUser.isPasswordExpired());
        assertNull(returnedUser.getGisuserrole());
    }

    @Test
    public void testCreateReturnMessage_authenticatedTrue_RoleTrue() {
        User returnedUser = ldapService.createReturnMessage(userName, true, true, true, attributesWithRole);
        assertEquals(userName, returnedUser.getUsername());
        assertTrue(returnedUser.isAuthenticated());
        assertTrue(returnedUser.isUnlocked());
        assertTrue(returnedUser.isPasswordExpired());
        assertEquals("GISUSER", returnedUser.getGisuserrole());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_noEntryHour() {
        ldapService.loginAttemptsMap.clear();
        ldapService.updateUserFailedLoginAttempts("bob");

        assertEquals(1, ldapService.loginAttemptsMap.get("bob").getAttempts());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_timeOverOneHour() {
        ldapService.loginAttemptsMap.clear();
        ldapService.loginAttemptsMap.put("bob", new LoginAttempts(1, LocalDateTime.now().minusHours(3)));
        ldapService.updateUserFailedLoginAttempts("bob");

        assertEquals(1, ldapService.loginAttemptsMap.get("bob").getAttempts());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_lessThanTwoFailed() {
        ldapService.loginAttemptsMap.clear();
        ldapService.loginAttemptsMap.put("bob", new LoginAttempts(1, LocalDateTime.now()));
        ldapService.updateUserFailedLoginAttempts("bob");

        assertEquals(2, ldapService.loginAttemptsMap.get("bob").getAttempts());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_lastAttempt() {
        LdapService localLdapService = spy(new LdapService(new Properties()));
        doNothing().when(localLdapService).lockUserAccount(anyString());

        localLdapService.loginAttemptsMap.clear();
        localLdapService.loginAttemptsMap.put("bob", new LoginAttempts(2, LocalDateTime.now()));
        localLdapService.updateUserFailedLoginAttempts("bob");

        verify(localLdapService, times(1)).lockUserAccount(anyString());
    }
}

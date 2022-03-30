package ca.bc.gov.hlth.ldapapi.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.naming.NamingException;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class LdapServiceTest {

    private static final LdapService ldapService = new LdapService(new Properties());

    @BeforeAll
    public static void initialSetup() {
        ldapService.attemptTimeout = 1;
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_noEntryHour() throws NamingException {
        ldapService.loginAttemptsMap.clear();
        ldapService.updateUserFailedLoginAttempts("bob");

        assertEquals(1, ldapService.loginAttemptsMap.get("bob").getAttempts());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_timeOverOneHour() throws NamingException {
        ldapService.loginAttemptsMap.clear();
        ldapService.loginAttemptsMap.put("bob", new LoginAttempts(1, LocalDateTime.now().minusHours(3)));
        ldapService.updateUserFailedLoginAttempts("bob");

        assertEquals(1, ldapService.loginAttemptsMap.get("bob").getAttempts());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_lessThanTwoFailed() throws NamingException {
        ldapService.loginAttemptsMap.clear();
        ldapService.loginAttemptsMap.put("bob", new LoginAttempts(1, LocalDateTime.now()));
        ldapService.updateUserFailedLoginAttempts("bob");

        assertEquals(2, ldapService.loginAttemptsMap.get("bob").getAttempts());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_lastAttempt() throws NamingException {
        LdapService localLdapService = spy(new LdapService(new Properties()));
        doNothing().when(localLdapService).lockUserAccount(anyString());

        localLdapService.loginAttemptsMap.clear();
        localLdapService.loginAttemptsMap.put("bob", new LoginAttempts(2, LocalDateTime.now()));
        localLdapService.updateUserFailedLoginAttempts("bob");

        verify(localLdapService, times(1)).lockUserAccount(anyString());
    }
}

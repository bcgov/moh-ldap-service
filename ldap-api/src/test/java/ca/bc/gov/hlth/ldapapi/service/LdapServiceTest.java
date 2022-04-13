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

    private static final UserService USER_SERVICE = new UserService(new Properties());

    @BeforeAll
    public static void initialSetup() {
        USER_SERVICE.attemptTimeout = 1;
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_noEntryHour() throws NamingException {
        USER_SERVICE.loginAttemptsMap.clear();
        USER_SERVICE.updateUserFailedLoginAttempts("bob");

        assertEquals(1, USER_SERVICE.loginAttemptsMap.get("bob").getAttempts());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_timeOverOneHour() throws NamingException {
        USER_SERVICE.loginAttemptsMap.clear();
        USER_SERVICE.loginAttemptsMap.put("bob", new LoginAttempts(1, LocalDateTime.now().minusHours(3)));
        USER_SERVICE.updateUserFailedLoginAttempts("bob");

        assertEquals(1, USER_SERVICE.loginAttemptsMap.get("bob").getAttempts());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_lessThanTwoFailed() throws NamingException {
        USER_SERVICE.loginAttemptsMap.clear();
        USER_SERVICE.loginAttemptsMap.put("bob", new LoginAttempts(1, LocalDateTime.now()));
        USER_SERVICE.updateUserFailedLoginAttempts("bob");

        assertEquals(2, USER_SERVICE.loginAttemptsMap.get("bob").getAttempts());
    }

    @Test
    public void testUpdateUserFailedLoginAttempts_lastAttempt() throws NamingException {
        UserService localUserService = spy(new UserService(new Properties()));
        doNothing().when(localUserService).lockUserAccount(anyString());

        localUserService.loginAttemptsMap.clear();
        localUserService.loginAttemptsMap.put("bob", new LoginAttempts(2, LocalDateTime.now()));
        localUserService.updateUserFailedLoginAttempts("bob");

        verify(localUserService, times(1)).lockUserAccount(anyString());
    }
}

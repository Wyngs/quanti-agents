package com.quantiagents.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.models.User;
import com.quantiagents.app.Services.UserService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UserFlowInstrumentedTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Make sure every test starts fresh.
        wipeProfile();
    }

    @After
    public void tearDown() {
        // Clean up any side-effects for the next run.
        wipeProfile();
    }

    @Test
    public void signUpStoresProfileAndHashesPassword() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        User user = userService.createUser("Alex Entrant", "alex@example.com", "5551112222", "secret123");
        assertNotNull(user);
        assertFalse(TextUtils.isEmpty(user.getDeviceId()));
        assertNotEquals("secret123", user.getPasswordHash());
    }

    @Test
    public void loginWithValidCredentialsSucceeds() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        userService.createUser("Sam Entrant", "sam@example.com", "5553334444", "samPass1");
        assertTrue(loginService.login("sam@example.com", "samPass1"));
    }

    @Test
    public void loginWithInvalidCredentialsFails() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        userService.createUser("Pat Entrant", "pat@example.com", "5556667777", "patPass1");
        assertFalse(loginService.login("pat@example.com", "badPass"));
    }

    @Test
    public void deviceLoginWorksAfterPasswordLogin() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        DeviceIdManager deviceIdManager = locator.deviceIdManager();
        userService.createUser("Dee Entrant", "dee@example.com", "5559998888", "deePass1");
        assertTrue(loginService.login("dee@example.com", "deePass1"));
        String deviceId = deviceIdManager.getDeviceId();
        // Recreate services to simulate a cold start.
        locator = new ServiceLocator(context);
        loginService = locator.loginService();
        assertTrue(loginService.loginWithDevice(deviceId));
    }

    @Test
    public void updateProfileAndPasswordPersists() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        userService.createUser("Kim Entrant", "kim@example.com", "5550001111", "kimPass1");
        assertTrue(loginService.login("kim@example.com", "kimPass1"));
        userService.updateUser("Kim Updated", "kim.updated@example.com", "5550002222");
        assertTrue(userService.updatePassword("newPass77"));
        loginService.logout();
        assertFalse(loginService.login("kim.updated@example.com", "kimPass1"));
        assertTrue(loginService.login("kim.updated@example.com", "newPass77"));
        User refreshed = userService.getCurrentUser();
        assertEquals("Kim Updated", refreshed.getName());
        assertEquals("kim.updated@example.com", refreshed.getEmail());
    }

    private void wipeProfile() {
        ServiceLocator locator = new ServiceLocator(context);
        locator.userService().deleteUserProfile();
        locator.deviceIdManager().reset();
    }
}

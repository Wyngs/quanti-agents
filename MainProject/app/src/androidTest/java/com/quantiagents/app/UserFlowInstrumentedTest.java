package com.quantiagents.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Covers the end-to-end profile stories: sign up, edit, delete, and device login.
 */
@RunWith(AndroidJUnit4.class)
public class UserFlowInstrumentedTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        wipeProfile();
    }

    @After
    public void tearDown() {
        wipeProfile();
    }

    @Test
    public void signUpStoresProfileAndHashesPassword() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        User user = createProfile(userService, "Alex Entrant", "AlexEntry", "alex@example.com", "5551112222", "secret123");
        assertNotNull(user);
        assertFalse(TextUtils.isEmpty(user.getDeviceId()));
        assertNotEquals("secret123", user.getPasswordHash());
    }

    @Test
    public void loginWithValidCredentialsSucceeds() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Sam Entrant", "SamEntry", "sam@example.com", "5553334444", "samPass1");
        assertTrue(loginService.login("sam@example.com", "samPass1"));
    }

    @Test
    public void loginWithInvalidCredentialsFails() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Pat Entrant", "PatEntry", "pat@example.com", "5556667777", "patPass1");
        assertFalse(loginService.login("pat@example.com", "badPass"));
    }

    @Test
    public void deviceLoginWorksAfterPasswordLogin() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Dee Entrant", "DeeEntry", "dee@example.com", "5559998888", "deePass1");
        assertTrue(loginService.login("dee@example.com", "deePass1"));
        User currentUser = userService.getCurrentUser();
        assertNotNull(currentUser);
        String deviceId = currentUser.getDeviceId();
        assertNotNull(deviceId);
        locator = new ServiceLocator(context);
        loginService = locator.loginService();
        assertTrue(loginService.loginWithDevice(deviceId));
    }

    @Test
    public void updateProfileAndPasswordPersists() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Kim Entrant", "KimEntry", "kim@example.com", "5550001111", "kimPass1");
        assertTrue(loginService.login("kim@example.com", "kimPass1"));

        CountDownLatch profileLatch = new CountDownLatch(1);
        AtomicReference<Exception> profileError = new AtomicReference<>();
        userService.updateUser("Kim Updated", "kim.updated@example.com", "5550002222",
                aVoid -> profileLatch.countDown(),
                e -> {
                    profileError.set(e);
                    profileLatch.countDown();
                });
        awaitLatch(profileLatch);
        if (profileError.get() != null) {
            fail("updateUser failed: " + profileError.get().getMessage());
        }

        CountDownLatch passLatch = new CountDownLatch(1);
        AtomicReference<Exception> passError = new AtomicReference<>();
        userService.updatePassword("newPass77",
                aVoid -> passLatch.countDown(),
                e -> {
                    passError.set(e);
                    passLatch.countDown();
                });
        awaitLatch(passLatch);
        if (passError.get() != null) {
            fail("updatePassword failed: " + passError.get().getMessage());
        }
        loginService.logout();

        // Poll until new password works
        boolean propagated = false;
        for (int i = 0; i < 20; i++) {
            if (loginService.login("kim.updated@example.com", "newPass77")) {
                propagated = true;
                loginService.logout();
                break;
            }
            SystemClock.sleep(200);
        }
        assertTrue("Updates did not propagate", propagated);

        // Assert old password fails and new works
        assertFalse(loginService.login("kim.updated@example.com", "kimPass1"));
        assertTrue(loginService.login("kim.updated@example.com", "newPass77"));

        User refreshed = waitForCurrentUser(userService);
        assertEquals("Kim Updated", refreshed.getName());
        assertEquals("kim.updated@example.com", refreshed.getEmail());
    }

    @Test
    public void deleteProfileClearsStoredUser() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Nico Entrant", "NicoEntry", "nico@example.com", "5554441212", "nicoPass1");
        assertTrue(loginService.login("nico@example.com", "nicoPass1"));

        CountDownLatch deleteLatch = new CountDownLatch(1);
        AtomicReference<Exception> deleteError = new AtomicReference<>();
        userService.deleteUserProfile(
                aVoid -> deleteLatch.countDown(),
                e -> {
                    deleteError.set(e);
                    deleteLatch.countDown();
                });
        awaitLatch(deleteLatch);

        if (deleteError.get() != null && !(deleteError.get() instanceof IllegalStateException)) {
            fail("deleteUserProfile failed: " + deleteError.get().getMessage());
        }
        assertTrue("Profile should be removed", waitForProfileRemoval(userService));
        assertNull(userService.getCurrentUser());
        assertFalse(loginService.login("nico@example.com", "nicoPass1"));
    }

    @Test
    public void logoutClearsSessionButKeepsProfile() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Ola Entrant", "olaEntry", "ola@example.com", "5559988777", "olaPass1");
        assertTrue(loginService.login("ola@example.com", "olaPass1"));
        loginService.logout();
        assertFalse(loginService.hasActiveSession());
        assertNotNull("Profile should remain after logout", userService.getCurrentUser());
        assertTrue(loginService.login("ola@example.com", "olaPass1"));
    }

    @Test
    public void createUserRejectsInvalidEmail() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        try {
            // This calls the synchronous version which validates inputs immediately
            userService.createUser("Bad Email", "BadUser", "not-an-email", "5551212121", "secret12");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void updateUserRequiresExistingProfile() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        try {
            userService.updateUser("Ghost", "ghost@example.com", "5551110000");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void asyncCreateUserInvokesCallback() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> created = new AtomicReference<>();
        userService.createUser("Async Entrant", "async", "async@example.com", "5551200987", "asyncPass1",
                user -> {
                    created.set(user);
                    latch.countDown();
                },
                e -> latch.countDown());
        awaitLatch(latch);
        assertNotNull("Callback user missing", created.get());
        assertFalse(TextUtils.isEmpty(created.get().getUserId()));
    }

    private void wipeProfile() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        CountDownLatch latch = new CountDownLatch(1);
        userService.deleteUserProfile(
                aVoid -> latch.countDown(),
                e -> latch.countDown()
        );
        awaitLatch(latch);
        waitForProfileRemoval(userService);
    }

    private User createProfile(UserService userService, String name, String username, String email, String phone, String password) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> createdRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        userService.createUser(name, username, email, phone, password,
                user -> {
                    createdRef.set(user);
                    latch.countDown();
                },
                e -> {
                    errorRef.set(e);
                    latch.countDown();
                });
        awaitLatch(latch);
        if (errorRef.get() != null) {
            fail("createUser failed: " + errorRef.get().getMessage());
        }
        assertNotNull("createUser callback missing", createdRef.get());
        User stored = waitForCurrentUser(userService);
        return stored;
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                fail("Timed out waiting for async task");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for async task");
        }
    }

    private User waitForCurrentUser(UserService userService) {
        for (int i = 0; i < 40; i++) {
            User user = userService.getCurrentUser();
            if (user != null && !TextUtils.isEmpty(user.getUserId())) {
                return user;
            }
            SystemClock.sleep(150);
        }
        return userService.getCurrentUser();
    }

    private boolean waitForProfileRemoval(UserService userService) {
        for (int i = 0; i < 40; i++) {
            if (userService.getCurrentUser() == null) {
                return true;
            }
            SystemClock.sleep(150);
        }
        return userService.getCurrentUser() == null;
    }
}
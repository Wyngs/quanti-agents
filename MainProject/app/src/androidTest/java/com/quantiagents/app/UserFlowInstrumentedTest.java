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
 * Covers the end-to-end profile stories I implemented: sign up, edit, delete, and device login.
 */
@RunWith(AndroidJUnit4.class)
public class UserFlowInstrumentedTest {

    private Context context;

    /**
     * Ensures each test starts without leftovers from previous runs.
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        wipeProfile();
    }

    /**
     * Extra cleanup pass just in case a test bails early.
     */
    @After
    public void tearDown() {
        wipeProfile();
    }

    /**
     * Verifies sign-up stores the new profile and hashes the password.
     */
    @Test
    public void signUpStoresProfileAndHashesPassword() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        User user = createProfile(userService, "Alex Entrant", "alex@example.com", "5551112222", "secret123");
        assertNotNull(user);
        assertFalse(TextUtils.isEmpty(user.getDeviceId()));
        assertNotEquals("secret123", user.getPasswordHash());
    }

    /**
     * Confirms the happy path login still works with real credentials.
     */
    @Test
    public void loginWithValidCredentialsSucceeds() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Sam Entrant", "sam@example.com", "5553334444", "samPass1");
        assertTrue(loginService.login("sam@example.com", "samPass1"));
    }

    /**
     * Sanity check that a wrong password fails fast.
     */
    @Test
    public void loginWithInvalidCredentialsFails() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Pat Entrant", "pat@example.com", "5556667777", "patPass1");
        assertFalse(loginService.login("pat@example.com", "badPass"));
    }

    /**
     * Makes sure the device-id login works after a normal credential login.
     */
    @Test
    public void deviceLoginWorksAfterPasswordLogin() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Dee Entrant", "dee@example.com", "5559998888", "deePass1");
        assertTrue(loginService.login("dee@example.com", "deePass1"));
        User currentUser = userService.getCurrentUser();
        assertNotNull(currentUser);
        String deviceId = currentUser.getDeviceId();
        assertNotNull(deviceId);
        locator = new ServiceLocator(context);
        loginService = locator.loginService();
        assertTrue(loginService.loginWithDevice(deviceId));
    }

    /**
     * Covers editing profile fields plus changing the password in one go.
     */
    @Test
    public void updateProfileAndPasswordPersists() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Kim Entrant", "kim@example.com", "5550001111", "kimPass1");
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

        // Poll until new password works to ensure Firestore collection query has caught up
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

        // Now assert old password fails
        assertFalse(loginService.login("kim.updated@example.com", "kimPass1"));
        // And confirm new password works again
        assertTrue(loginService.login("kim.updated@example.com", "newPass77"));

        User refreshed = waitForCurrentUser(userService);
        assertEquals("Kim Updated", refreshed.getName());
        assertEquals("kim.updated@example.com", refreshed.getEmail());
    }

    /**
     * Deletes the profile and ensures the cached session is gone.
     */
    @Test
    public void deleteProfileClearsStoredUser() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Nico Entrant", "nico@example.com", "5554441212", "nicoPass1");
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

    /**
     * Shows that logging out only clears the session, not the stored profile.
     */
    @Test
    public void logoutClearsSessionButKeepsProfile() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        createProfile(userService, "Ola Entrant", "ola@example.com", "5559988777", "olaPass1");
        assertTrue(loginService.login("ola@example.com", "olaPass1"));
        loginService.logout();
        assertFalse(loginService.hasActiveSession());
        assertNotNull("Profile should remain after logout", userService.getCurrentUser());
        // user can immediately login again
        assertTrue(loginService.login("ola@example.com", "olaPass1"));
    }

    /**
     * Verifies invalid emails are rejected during sign-up.
     */
    @Test
    public void createUserRejectsInvalidEmail() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        try {
            userService.createUser("Bad Email", "not-an-email", "5551212121", "secret12");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    /**
     * Updating before a profile exists should throw.
     */
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

    /**
     * Ensures the async createUser callback fires and returns the created profile.
     */
    @Test
    public void asyncCreateUserInvokesCallback() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> created = new AtomicReference<>();
        userService.createUser("Async Entrant", "async@example.com", "5551200987", "asyncPass1",
                user -> {
                    created.set(user);
                    latch.countDown();
                },
                e -> latch.countDown());
        awaitLatch(latch);
        assertNotNull("Callback user missing", created.get());
        assertFalse(TextUtils.isEmpty(created.get().getUserId()));
    }

    /**
     * getCurrentUserFresh should mirror the cached getter once Firestore settles.
     */
    @Test
    public void getCurrentUserFreshMatchesCachedUser() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        User cached = createProfile(userService, "Fresh Entrant", "fresh@example.com", "5558881212", "freshPass1");
        User fresh = waitForCurrentUser(userService);
        assertEquals(cached.getUserId(), fresh.getUserId());
        User server = userService.getCurrentUserFresh();
        assertNotNull(server);
        assertEquals(cached.getUserId(), server.getUserId());
    }

    /**
     * authenticateDevice returns false when the device id doesn't match any profile.
     */
    @Test
    public void authenticateDeviceRejectsUnknownId() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        assertFalse(userService.authenticateDevice("bogus-device"));
    }

    /**
     * deleteUserProfile async path surfaces an error when no profile exists.
     */
    @Test
    public void asyncDeleteProfileFailsWhenMissing() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();
        userService.deleteUserProfile(
                aVoid -> latch.countDown(),
                e -> {
                    failure.set(e);
                    latch.countDown();
                });
        awaitLatch(latch);
        assertNotNull("Expected failure callback", failure.get());
    }

    /**
     * Removes any stored profile and waits for Firestore to settle.
     */
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

    /**
     * Convenience method to create a user and wait for the callback synchronously.
     */
    private User createProfile(UserService userService,
                               String name,
                               String email,
                               String phone,
                               String password) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> createdRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        userService.createUser(name, email, phone, password,
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
        assertNotNull("Current user should be available", stored);
        return stored;
    }

    /**
     * Shared helper to keep async tests deterministic.
     */
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

    /**
     * Polls until a non-null current user is available.
     */
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

    /**
     * Polls until the current user reference disappears.
     */
    private boolean waitForProfileRemoval(UserService userService) {
        for (int i = 0; i < 40; i++) {
            if (userService.getCurrentUser() == null) {
                return true;
            }
            SystemClock.sleep(150);
        }
        return userService.getCurrentUser() == null;
    }

    /**
     * Polls using the server-backed getter to see if the notification flag matches expectation.
     */
    private boolean waitForNotificationState(UserService userService, boolean expected) {
        for (int i = 0; i < 40; i++) {
            User user = userService.getCurrentUserFresh();
            if (user != null && user.hasNotificationsOn() == expected) {
                return true;
            }
            SystemClock.sleep(150);
        }
        User finalUser = userService.getCurrentUserFresh();
        return finalUser != null && finalUser.hasNotificationsOn() == expected;
    }
}
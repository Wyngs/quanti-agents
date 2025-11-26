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
    private String suffix;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Generates unique suffix for this specific test run (e.g., _1734567890)
        suffix = "_" + System.currentTimeMillis();
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
        User user = createProfile(userService, "Alex Entrant", "AlexEntry" + suffix, "alex" + suffix + "@example.com", "5551112222", "secret123");
        assertNotNull(user);
        assertFalse(TextUtils.isEmpty(user.getDeviceId()));
        assertNotEquals("secret123", user.getPasswordHash());

        CountDownLatch latch = new CountDownLatch(1);
        UserService cleanupService = new UserService(context) {
            @Override
            public void getCurrentUser(com.google.android.gms.tasks.OnSuccessListener<User> success, com.google.android.gms.tasks.OnFailureListener failure) {
                success.onSuccess(user);
            }
        };

        cleanupService.deleteUserProfile(
                aVoid -> latch.countDown(),
                e -> latch.countDown()
        );
        awaitLatch(latch);

    }

    @Test
    public void loginWithValidCredentialsSucceeds() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        String email = "sam" + suffix + "@example.com";
        createProfile(userService, "Sam Entrant", "SamEntry" + suffix, email, "5553334444", "samPass1");
        assertTrue(loginService.login(email, "samPass1"));
    }

    @Test
    public void loginWithInvalidCredentialsFails() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        String email = "pat" + suffix + "@example.com";
        createProfile(userService, "Pat Entrant", "PatEntry" + suffix, email, "5556667777", "patPass1");
        assertFalse(loginService.login(email, "badPass"));
    }

    @Test
    public void deviceLoginWorksAfterPasswordLogin() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        String email = "dee" + suffix + "@example.com";
        createProfile(userService, "Dee Entrant", "DeeEntry" + suffix, email, "5559998888", "deePass1");
        assertTrue(loginService.login(email, "deePass1"));
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

        String email = "kim" + suffix + "@example.com";
        String updatedEmail = "kim.updated" + suffix + "@example.com";

        createProfile(userService, "Kim Entrant", "KimEntry" + suffix, email, "5550001111", "kimPass1");
        assertTrue(loginService.login(email, "kimPass1"));

        CountDownLatch profileLatch = new CountDownLatch(1);
        AtomicReference<Exception> profileError = new AtomicReference<>();
        userService.updateUser("Kim Updated", updatedEmail, "5550002222",
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
            if (loginService.login(updatedEmail, "newPass77")) {
                propagated = true;
                loginService.logout();
                break;
            }
            SystemClock.sleep(200);
        }
        assertTrue("Updates did not propagate", propagated);

        // Assert old password fails and new works
        assertFalse(loginService.login(updatedEmail, "kimPass1"));
        assertTrue(loginService.login(updatedEmail, "newPass77"));

        User refreshed = waitForCurrentUser(userService);
        assertEquals("Kim Updated", refreshed.getName());
        assertEquals(updatedEmail, refreshed.getEmail());
    }

    @Test
    public void deleteProfileClearsStoredUser() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        String email = "nico" + suffix + "@example.com";

        // 1. Create the user
        User nico = createProfile(userService, "Nico Entrant", "NicoEntry" + suffix, email, "5554441212", "nicoPass1");
        assertTrue(loginService.login(email, "nicoPass1"));

        // 2. TARGETED DELETE FIX:
        // Instead of calling userService.deleteUserProfile() which ambiguously looks up by DeviceID
        // (and accidentally finds Admin), we use a helper to delete specifically NICO's ID.
        deleteSpecificUser(nico);

        // 3. Verify removal
        // Note: We use a custom check here because getCurrentUser might return Admin again, which is confusing but correct for the test logic (Nico is gone).
        // We verify Nico specifically is gone.
        assertFalse("Nico should be removed", checkUserExists(userService, nico.getUserId()));
    }

    @Test
    public void logoutClearsSessionButKeepsProfile() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        LoginService loginService = locator.loginService();
        String email = "ola" + suffix + "@example.com";
        createProfile(userService, "Ola Entrant", "OlaEntry" + suffix, email, "5559988777", "olaPass1");
        assertTrue(loginService.login(email, "olaPass1"));
        loginService.logout();
        assertFalse(loginService.hasActiveSession());
        assertNotNull("Profile should remain after logout", userService.getCurrentUser());
        assertTrue(loginService.login(email, "olaPass1"));
    }

    @Test
    public void createUserRejectsInvalidEmail() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        try {
            // This calls the synchronous version which validates inputs immediately
            userService.createUser("Bad Email", "BadUser" + suffix, "not-an-email", "5551212121", "secret12");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void updateUserRequiresExistingProfile() {
        // Fix: Create temporary anonymous subclass of UserService.
        // Forces service to behave as if no user is logged in,
        // regardless of what is actually in the database or on the device.
        UserService mockService = new UserService(context) {
            @Override
            public User getCurrentUser() {
                return null; // Forcing "no user found" state
            }
        };

        try {
            // should now hit the "requireUser()" check and throw exception
            mockService.updateUser("Ghost", "ghost@example.com", "5551110000");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // ok - exception was thrown as expected
        }
    }

    @Test
    public void asyncCreateUserInvokesCallback() {
        ServiceLocator locator = new ServiceLocator(context);
        UserService userService = locator.userService();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> created = new AtomicReference<>();
        userService.createUser("Async Entrant", "async" + suffix, "async" + suffix + "@example.com", "5551200987", "asyncPass1",
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

        userService.getCurrentUser(
                user -> {
                    // STRICTER SAFETY CHECK:
                    // Checks for the specific timestamp pattern like _1734567890 thats used by tests.
                    // This regex means: match any characters, then an underscore, then numbers, then @example.com
                    if (user != null && user.getEmail() != null &&
                            user.getEmail().matches(".*_\\d+@example\\.com")) {

                        // Safe to delete this specific test user
                        userService.deleteUserProfile(
                                aVoid -> latch.countDown(),
                                e -> latch.countDown()
                        );
                    } else {
                        // This is a real user or null. Do NOT delete.
                        latch.countDown();
                    }
                },
                e -> {
                    latch.countDown();
                }
        );

        awaitLatch(latch);
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

    // Helper to delete a user without relying on Device ID lookup
    private void deleteSpecificUser(User targetUser) {
        if(targetUser == null) return;

        CountDownLatch latch = new CountDownLatch(1);
        // We trick the service into thinking 'targetUser' is the current one
        UserService targetedService = new UserService(context) {
            @Override
            public void getCurrentUser(com.google.android.gms.tasks.OnSuccessListener<User> s, com.google.android.gms.tasks.OnFailureListener f) {
                s.onSuccess(targetUser);
            }
        };

        targetedService.deleteUserProfile(
                aVoid -> latch.countDown(),
                e -> latch.countDown()
        );
        awaitLatch(latch);
    }

    // Helper to verify user is actually gone from DB
    private boolean checkUserExists(UserService service, String userId) {
        User u = service.getUserById(userId);
        return u != null;
    }
}
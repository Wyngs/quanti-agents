package com.quantiagents.app;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.Services.NotificationService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.Services.AdminService;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.ImageService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.User;
import com.quantiagents.app.models.Notification;
import com.quantiagents.app.models.UserSummary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AdminFlowInstrumentedTest {

    private Context context;
    private ServiceLocator locator;

    // New unique IDs for this specific test run for isolation
    private String testEvt1Id;
    private String testEvt2Id;
    private String testImg1Id;
    private String testImg2Id;
    private String testUserId;
    private String testUser2Id;

    private String cascadeOrgId;
    private String cascadeEvtId;
    private String notifOrgId;
    private String notifEvtId;
    private String notifImgId;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        locator = new ServiceLocator(context);

        // Generate unique random IDs for test run
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testEvt1Id = "test_evt1_" + uniqueSuffix;
        testEvt2Id = "test_evt2_" + uniqueSuffix;
        testImg1Id = "test_img1_" + uniqueSuffix;
        testImg2Id = "test_img2_" + uniqueSuffix;
        testUserId = "test_user_" + uniqueSuffix;
        testUser2Id = "test_user2_" + uniqueSuffix;

        cascadeOrgId = "cascade_org_" + uniqueSuffix;
        cascadeEvtId = "cascade_evt_" + uniqueSuffix;
        notifOrgId = "notif_org_" + uniqueSuffix;
        notifEvtId = "notif_evt_" + uniqueSuffix;
        notifImgId = "notif_img_" + uniqueSuffix;

        seedUniqueData();
    }

    @After
    public void tearDown() {
        // Only clean up specific objects created
        cleanupTestObjects();
    }

    @Test
    public void adminDeleteFlows() {
        AdminService admin = locator.adminService();

        // --- Events & Images ---

        // Delete Event 1. should cascade and delete Image 1.
        // NEW: Updated to use 5-argument async signature (id, confirmed, note, success, failure) to match AdminService
        assertTrue("Failed to remove event 1", removeEventSync(admin, testEvt1Id, true, "Test deletion"));

        // NEW: listEventsSync uses callbacks internally to fetch data async
        List<Event> allEvents = listEventsSync(admin);
        // NEW: listImagesSync uses callbacks internally to fetch data async
        List<Image> allImages = listImagesSync(admin);

        // Verify Event 1 is gone, but Event 2 remains
        assertFalse("Event 1 should be deleted", containsEvent(allEvents, testEvt1Id));
        assertTrue("Event 2 should remain", containsEvent(allEvents, testEvt2Id));

        // Verify Image 1 (linked to Event 1) is gone
        assertFalse("Image 1 (cascade) should be deleted", containsImage(allImages, testImg1Id));
        // Verify Image 2 (standalone) remains
        assertTrue("Image 2 should remain", containsImage(allImages, testImg2Id));


        //Profiles

        // Verify we can delete the specific test user we created
        // NEW: Updated helper to use async signature
        assertTrue("Failed to remove profile", removeProfileSync(admin, testUserId, true, "Test deletion"));

        // Verify test user is gone
        // NEW: listProfilesSync now uses callbacks internally and returns List<User>
        List<User> allUsers = listProfilesSync(admin);
        assertFalse("Test User 1 should be deleted", containsUser(allUsers, testUserId));
        assertTrue("Test User 2 should remain", containsUser(allUsers, testUser2Id));


        //standalone images

        // Delete the remaining standalone image
        // NEW: Updated helper to use async signature
        assertTrue("Failed to remove image 2", removeImageSync(admin, testImg2Id, true, "Test deletion"));

        // Verify it's gone
        allImages = listImagesSync(admin);
        assertFalse("Image 2 should be deleted", containsImage(allImages, testImg2Id));
    }

    // Testing cascade deletion (Organizer -> Events)
    @Test
    public void adminDeleteOrganizerCascades() {
        AdminService admin = locator.adminService();

        List<User> users = listProfilesSync(admin);
        List<Event> events = listEventsSync(admin);

        assertTrue("Cascade Organizer should exist before delete", containsUser(users, cascadeOrgId));
        assertTrue("Cascade Event should exist before delete", containsEvent(events, cascadeEvtId));

        assertTrue("Failed to remove organizer", removeProfileSync(admin, cascadeOrgId, true, "Cascade Test"));

        // Sleep briefly to allow cloud functions/async triggers to complete
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        users = listProfilesSync(admin);
        events = listEventsSync(admin);

        assertFalse("Organizer should be deleted", containsUser(users, cascadeOrgId));
        assertFalse("Orphaned Event should be deleted", containsEvent(events, cascadeEvtId));
    }

    // Testing Event Deletion Notification
    @Test
    public void adminEventDeletionGeneratesNotification() {
        AdminService admin = locator.adminService();
        NotificationService notifService = locator.notificationService();

        assertTrue(removeEventSync(admin, notifEvtId, true, "Notification Test"));

        // Sleep briefly to allow notification creation
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        List<Notification> notifications = listNotificationsSync(notifService);
        int expectedRecipient = Math.abs(notifOrgId.hashCode());

        boolean found = false;
        for (Notification n : notifications) {
            if (n.getRecipientId() == expectedRecipient &&
                    n.getType() == constant.NotificationType.BAD &&
                    n.getStatus().contains("Canceled")) {
                found = true;
                break;
            }
        }
        assertTrue("Organizer should receive cancellation notification", found);
    }

    // Testing Image Deletion Notification
    @Test
    public void adminImageDeletionGeneratesNotification() {
        AdminService admin = locator.adminService();
        NotificationService notifService = locator.notificationService();

        assertTrue(removeImageSync(admin, notifImgId, true, "Notification Test"));

        // Sleep briefly to allow notification creation
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        List<Notification> notifications = listNotificationsSync(notifService);
        int expectedRecipient = Math.abs(notifOrgId.hashCode());

        boolean found = false;
        for (Notification n : notifications) {
            if (n.getRecipientId() == expectedRecipient &&
                    n.getType() == constant.NotificationType.BAD &&
                    n.getStatus().contains("Image removed")) {
                found = true;
                break;
            }
        }
        assertTrue("Organizer should receive image removal notification", found);
    }

    //Helper methods for seeding

    private void seedUniqueData() {
        EventService eventService = locator.eventService();
        ImageService imageService = locator.imageService();
        // Use UserRepository directly to bypass auth/session logic for test seeding
        UserRepository userRepository = new UserRepository(new FireBaseRepository());

        //1. Create Events
        Event evt1 = new Event(testEvt1Id, "Test Event 1", testImg1Id);
        Event evt2 = new Event(testEvt2Id, "Test Event 2", null);
        saveEventSync(eventService, evt1);
        saveEventSync(eventService, evt2);

        //2. Create Images
        //img1 is linked to evt1, img2 is standalone
        Image img1 = new Image(testImg1Id, testEvt1Id, "uri://test1");
        Image img2 = new Image(testImg2Id, null, "uri://test2");
        saveImageSync(imageService, img1);
        saveImageSync(imageService, img2);

        //3. Create Users
        User user1 = new User(testUserId, "device1", "Test User 1", "username1", "test1@example.com", "123", "pass");
        User user2 = new User(testUser2Id, "device2", "Test User 2", "username2", "test2@example.com", "456", "pass");

        saveUserSync(userRepository, user1);
        saveUserSync(userRepository, user2);

        // Seed Cascade Data
        User orgUser = new User(cascadeOrgId, "device_c", "Cascade Org", "org_c", "org@c.com", "000", "pass");
        saveUserSync(userRepository, orgUser);
        Event orgEvt = new Event(cascadeEvtId, "Cascade Event", null);
        orgEvt.setOrganizerId(cascadeOrgId);
        saveEventSync(eventService, orgEvt);

        // Seed Notification Data
        User notifUser = new User(notifOrgId, "device_n", "Notif Org", "org_n", "org@n.com", "000", "pass");
        saveUserSync(userRepository, notifUser);
        Event notifEvt = new Event(notifEvtId, "Notif Event", notifImgId);
        notifEvt.setOrganizerId(notifOrgId);
        notifEvt.setWaitingList(new java.util.ArrayList<>());
        saveEventSync(eventService, notifEvt);
        Image notifImg = new Image(notifImgId, notifEvtId, "uri://notif");
        saveImageSync(imageService, notifImg);
    }

    private void cleanupTestObjects() {
        AdminService admin = locator.adminService();

        // Trying to delete everything we created
        // NEW: Added try-catch blocks to prevent crashes if items were already deleted during test
        try { removeEventSync(admin, testEvt2Id, true, null); } catch (Exception ignored) {}
        try { removeEventSync(admin, testEvt1Id, true, null); } catch (Exception ignored) {}

        try { removeProfileSync(admin, testUser2Id, true, null); } catch (Exception ignored) {}
        try { removeProfileSync(admin, testUserId, true, null); } catch (Exception ignored) {}

        try { removeImageSync(admin, testImg1Id, true, null); } catch (Exception ignored) {}
        try { removeImageSync(admin, testImg2Id, true, null); } catch (Exception ignored) {}

        try { removeEventSync(admin, cascadeEvtId, true, null); } catch (Exception ignored) {}
        try { removeProfileSync(admin, cascadeOrgId, true, null); } catch (Exception ignored) {}
        try { removeEventSync(admin, notifEvtId, true, null); } catch (Exception ignored) {}
        try { removeProfileSync(admin, notifOrgId, true, null); } catch (Exception ignored) {}
        try { removeImageSync(admin, notifImgId, true, null); } catch (Exception ignored) {}
    }

    // Sync wrappers for async Calls

    private boolean removeEventSync(AdminService admin, String eventId, boolean confirmed, String note) {
        CountDownLatch latch = new CountDownLatch(1);
        // Using AtomicBoolean for thread safety
        AtomicBoolean success = new AtomicBoolean(false);
        admin.removeEvent(eventId, confirmed, note,
                s -> { success.set(true); latch.countDown(); },
                f -> latch.countDown()
        );
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { return false; }
        return success.get();
    }

    private boolean removeProfileSync(AdminService admin, String userId, boolean confirmed, String note) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        admin.removeProfile(userId, confirmed, note,
                s -> { success.set(true); latch.countDown(); },
                f -> latch.countDown()
        );
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { return false; }
        return success.get();
    }

    private boolean removeImageSync(AdminService admin, String imageId, boolean confirmed, String note) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        // NEW: Updated to use 5-arg async signature
        admin.removeImage(imageId, confirmed, note,
                s -> { success.set(true); latch.countDown(); },
                f -> latch.countDown()
        );
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { return false; }
        return success.get();
    }

    private List<Event> listEventsSync(AdminService admin) {
        CountDownLatch latch = new CountDownLatch(1);
        // Usign AtomicReference for thread safety
        AtomicReference<List<Event>> result = new AtomicReference<>();

        admin.getAllEvents(
                list -> { result.set(list); latch.countDown(); },
                e -> latch.countDown()
        );
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return result.get();
    }

    private List<User> listProfilesSync(AdminService admin) {
        CountDownLatch latch = new CountDownLatch(1);
        // Using AtomicReference for thread safety
        AtomicReference<List<User>> result = new AtomicReference<>();

        admin.listAllProfiles(
                list -> { result.set(list); latch.countDown(); },
                e -> latch.countDown()
        );
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return result.get();
    }

    private List<Image> listImagesSync(AdminService admin) {
        CountDownLatch latch = new CountDownLatch(1);
        // Using AtomicReference for thread safety
        AtomicReference<List<Image>> result = new AtomicReference<>();

        admin.listAllImages(
                list -> { result.set(list); latch.countDown(); },
                e -> latch.countDown()
        );
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return result.get();
    }

    // Helper to fetch notifications synchronously on background thread
    private List<Notification> listNotificationsSync(NotificationService service) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<Notification>> result = new AtomicReference<>();

        new Thread(() -> {
            try {
                List<Notification> list = service.getAllNotifications();
                result.set(list);
            } catch (Exception ignored) {}
            latch.countDown();
        }).start();

        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return result.get() != null ? result.get() : java.util.Collections.emptyList();
    }

    // Sync savers

    private void saveEventSync(EventService service, Event event) {
        CountDownLatch latch = new CountDownLatch(1);
        service.saveEvent(event, id -> latch.countDown(), e -> latch.countDown());
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) {}
    }

    private void saveImageSync(ImageService service, Image image) {
        CountDownLatch latch = new CountDownLatch(1);
        service.saveImage(image, id -> latch.countDown(), e -> latch.countDown());
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) {}
    }

    private void saveUserSync(UserRepository repo, User user) {
        CountDownLatch latch = new CountDownLatch(1);
        repo.saveUser(user, v -> latch.countDown(), e -> latch.countDown());
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) {}
    }

    // Assertion Helpers

    private boolean containsEvent(List<Event> list, String id) {
        if (list == null) return false;
        for (Event e : list) {
            if (e.getEventId().equals(id)) return true;
        }
        return false;
    }

    private boolean containsImage(List<Image> list, String id) {
        if (list == null) return false;
        for (Image i : list) {
            if (i.getImageId().equals(id)) return true;
        }
        return false;
    }

    private boolean containsUser(List<User> list, String id) {
        if (list == null) return false;
        for (User u : list) {
            if (u.getUserId().equals(id)) return true;
        }
        return false;
    }
}
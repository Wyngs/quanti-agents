package com.quantiagents.app;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Repository.FireBaseRepository;
import com.quantiagents.app.Repository.UserRepository;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.Services.AdminService;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.ImageService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID; // added
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AdminFlowInstrumentedTest {

    private Context context;
    private ServiceLocator locator;

    //new unique IDs for this specific test run to ensure isolation
    private String testEvt1Id;
    private String testEvt2Id;
    private String testImg1Id;
    private String testImg2Id;
    private String testUserId;
    private String testUser2Id;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        locator = new ServiceLocator(context);

        // wipe(); // commented out old wipe
        // seed(); // commented out old seed

        // generate unique random IDs for this test run
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testEvt1Id = "test_evt1_" + uniqueSuffix;
        testEvt2Id = "test_evt2_" + uniqueSuffix;
        testImg1Id = "test_img1_" + uniqueSuffix;
        testImg2Id = "test_img2_" + uniqueSuffix;
        testUserId = "test_user_" + uniqueSuffix;
        testUser2Id = "test_user2_" + uniqueSuffix;

        // only seed unique test data
        seedUniqueData();
    }

    @After
    public void tearDown() {
        //clean up for the next run
        // wipe(); // commented out old wipe

        // only clean up the specific objects we created
        cleanupTestObjects();
    }

    @Test
    public void adminDeleteFlows() throws InterruptedException {
        AdminService admin = locator.adminService();
        EventService eventService = locator.eventService();
        ImageService imageService = locator.imageService();
        UserService userService = locator.userService();

        // 1. Seed Data
        String evtId = "evt_admin_test";
        String imgId = "img_admin_test";
        String userId = "user_admin_test";

        CountDownLatch seedLatch = new CountDownLatch(3);

        // Save Event
        Event evt = new Event();
        evt.setEventId(evtId);
        evt.setTitle("Admin Test Event");
        eventService.saveEvent(evt, s -> seedLatch.countDown(), e -> seedLatch.countDown());

        // Save Image
        Image img = new Image(imgId, evtId, "http://dummy");
        imageService.saveImage(img, s -> seedLatch.countDown(), e -> seedLatch.countDown());

        // Save User
        userService.createUser("Admin Target", "target@test.com", null, "password",
                u -> {
                    u.setUserId(userId); // Ensure ID matches if generated differs
                    seedLatch.countDown();
                },
                e -> seedLatch.countDown());

        /*
        //us 03.01.01 b–c: delete an event and cascade its images
        String evt1Id = "evt1";
        assertTrue(admin.removeEvent(evt1Id, true, "t"));
        List<Event> events = admin.listAllEvents();
        assertEquals(1, events.size());                   //evt2 remains
        assertEquals(1, admin.listAllImages().size());    //img1 removed, img2 remains

        // 2. Test Remove Event
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicBoolean eventDeleted = new AtomicBoolean(false);

        admin.removeEvent(evtId, true, "test delete",
                aVoid -> {
                    eventDeleted.set(true);
                    eventLatch.countDown();
                },
                e -> eventLatch.countDown());

        assertTrue(eventLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Event should be deleted", eventDeleted.get());
        assertNull("Event should not exist in service", eventService.getEventById(evtId));

        // 3. Test Remove Profile
        // We need to find the user first to get the exact ID if it was auto-generated,
        // but for this test we assume we passed or captured the ID.
        CountDownLatch userLatch = new CountDownLatch(1);
        AtomicBoolean userDeleted = new AtomicBoolean(false);

        //us 03.03.01 b–c: delete the remaining image
        // UPDATED: Uses helper method now because removeImage is now asynchronous
        assertTrue(removeImageSync(admin, "img2", true, "t"));
        assertEquals(0, admin.listAllImages().size());
        */

        // --- NEW ISOLATED TEST LOGIC ---

        //us 03.01.01 b–c: delete an event and cascade its images
        // delete Event 1. should cascade and delete Image 1.
        assertTrue(admin.removeEvent(testEvt1Id, true, "Test deletion"));

        List<Event> allEvents = admin.listAllEvents();
        List<Image> allImages = admin.listAllImages();

        // verify Event 1 is gone, but Event 2 remains
        assertFalse("Event 1 should be deleted", containsEvent(allEvents, testEvt1Id));
        assertTrue("Event 2 should remain", containsEvent(allEvents, testEvt2Id));

        // verify Image 1 (linked to Event 1) is gone
        assertFalse("Image 1 (cascade) should be deleted", containsImage(allImages, testImg1Id));
        // verify Image 2 (standalone) remains
        assertTrue("Image 2 should remain", containsImage(allImages, testImg2Id));

        //us 03.02.01 b–c: delete a profile;
        // verify we can delete the specific test user created
        assertTrue(removeProfileSync(admin, testUserId, true, "Test deletion"));

        // verify test user is gone
        List<UserSummary> allProfiles = admin.listAllProfiles();
        assertFalse("Test User 1 should be deleted", containsProfile(allProfiles, testUserId));
        assertTrue("Test User 2 should remain", containsProfile(allProfiles, testUser2Id));


        //us 03.03.01 b–c: delete the remaining image
        // delete remaining standalone image
        assertTrue(removeImageSync(admin, testImg2Id, true, "Test deletion"));

        // verify it's gone
        allImages = admin.listAllImages();
        assertFalse("Image 2 should be deleted", containsImage(allImages, testImg2Id));
    }

    /*
    private void wipe() {
        // ... old wipe code ...
    }

    private void seed() {
        // ... old seed code ...
    }
    */

    // new helpers for isolated testing

    private void seedUniqueData() {
        EventService eventService = locator.eventService();
        ImageService imageService = locator.imageService();
        // use UserRepository directly to bypass auth/session logic for test seeding
        UserRepository userRepository = new UserRepository(new FireBaseRepository());

        // 1. creating Events
        Event evt1 = new Event(testEvt1Id, "Test Event 1", testImg1Id);
        Event evt2 = new Event(testEvt2Id, "Test Event 2", null);
        saveEventSync(eventService, evt1);
        saveEventSync(eventService, evt2);

        // 2. creating Images
        // img1 is linked to evt1, img2 is standalone
        Image img1 = new Image(testImg1Id, testEvt1Id, "uri://test1");
        Image img2 = new Image(testImg2Id, null, "uri://test2");
        saveImageSync(imageService, img1);
        saveImageSync(imageService, img2);

        // 3. creating Users
        User user1 = new User(testUserId, "device1", "Test User 1", "test1@example.com", "123", "pass");
        User user2 = new User(testUser2Id, "device2", "Test User 2", "test2@example.com", "456", "pass");

        saveUserSync(userRepository, user1);
        saveUserSync(userRepository, user2);
    }

    private void cleanupTestObjects() {
        AdminService admin = locator.adminService();
        try {
            admin.removeEvent(testEvt2Id, true, null); // event 2 wasn't deleted in test
        } catch (Exception ignored) {}

        // clean up profiles
        removeProfileSync(admin, testUser2Id, true, null);

        // clean up anything else that might have been left behind if test failed midway
        try { admin.removeEvent(testEvt1Id, true, null); } catch (Exception ignored) {}
        removeImageSync(admin, testImg1Id, true, null);
        removeImageSync(admin, testImg2Id, true, null);
        removeProfileSync(admin, testUserId, true, null);
    }

    // assertion helpers
    // these check if an item exists in the list by ID, ignoring all other data in DB

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

    private boolean containsProfile(List<UserSummary> list, String id) {
        if (list == null) return false;
        for (UserSummary u : list) {
            if (u.getUserId().equals(id)) return true;
        }
        return false;
    }

        assertTrue(userLatch.await(5, TimeUnit.SECONDS));
        assertTrue("User should be deleted", userDeleted.get());

        // 4. Test Remove Image
        CountDownLatch imgLatch = new CountDownLatch(1);
        AtomicBoolean imgDeleted = new AtomicBoolean(false);

        admin.removeImage(imgId, true, "test delete img",
                aVoid -> {
                    imgDeleted.set(true);
                    imgLatch.countDown();
                },
                failure -> {
                    result[0] = false;
                    latch.countDown();
                }
        );

        try {
            // Waits for up to 5 seconds for the database to respond
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return result[0];
    }

    /**
     * NEW HELPER FUNCTION (updated to match return type) : Wraps async removeImage call in a synchronous helper for testing
     */
    private boolean removeImageSync(AdminService admin, String imageId, boolean confirmed, String note) {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        admin.removeImage(imageId, confirmed, note,
                success -> {
                    result[0] = true;
                    latch.countDown();
                },
                failure -> {
                    result[0] = false;
                    latch.countDown();
                }
        );
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return result[0];
    }

    // Helper method to save event synchronously
    private void saveEventSync(EventService eventService, Event event) {
        CountDownLatch latch = new CountDownLatch(1);
        eventService.saveEvent(event,
                aVoid -> latch.countDown(),
                e -> latch.countDown());
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Helper method to save image synchronously
    private void saveImageSync(ImageService imageService, Image image) {
        CountDownLatch latch = new CountDownLatch(1);
        imageService.saveImage(image,
                aVoid -> latch.countDown(),
                e -> latch.countDown());
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Helper method to save user synchronously (Added for this fix)
    private void saveUserSync(UserRepository repo, User user) {
        CountDownLatch latch = new CountDownLatch(1);
        repo.saveUser(user,
                aVoid -> latch.countDown(),
                e -> latch.countDown());
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
}
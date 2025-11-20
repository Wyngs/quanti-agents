package com.quantiagents.app;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.Services.AdminService;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.ImageService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.Repository.ProfilesRepository;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.Image;
import com.quantiagents.app.models.User;
import com.quantiagents.app.models.UserSummary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class AdminFlowInstrumentedTest {

    private Context context;
    private ServiceLocator locator;

    @Before
    public void setUp() {
        //always start from a clean slate
        context = ApplicationProvider.getApplicationContext();
        locator = new ServiceLocator(context);
        wipe();
        seed();
    }

    @After
    public void tearDown() {
        //clean up for the next run
        wipe();
    }

    @Test
    public void adminStories0301to0303Work() {
        AdminService admin = locator.adminService();

        //us 03.01.01 b–c: delete an event and cascade its images
        String evt1Id = "evt1";
        assertTrue(admin.removeEvent(evt1Id, true, "t"));
        List<Event> events = admin.listAllEvents();
        assertEquals(1, events.size());                   //evt2 remains
        assertEquals(1, admin.listAllImages().size());    //img1 removed, img2 remains

        //us 03.02.01 b–c: delete a profile; if it is our local user, local profile is cleared
        User local = locator.userService().getCurrentUser();
        assertNotNull(local);

        assertTrue(removeProfileSync(admin, local.getUserId(), true, "t"));

        // Fix: Wait for async deletion to propagate
        boolean removed = waitForProfileRemoval(locator.userService());
        assertTrue("Local user profile should be removed", removed);

        assertEquals(1, admin.listAllProfiles().size());  //u2 remains

        //us 03.03.01 b–c: delete the remaining image
        assertTrue(admin.removeImage("img2", true, "t"));
        assertEquals(0, admin.listAllImages().size());
    }

    private void wipe() {
        //remove all events
        EventService eventService = locator.eventService();
        List<Event> events = eventService.getAllEvents();
        for (Event e : events) {
            eventService.deleteEvent(e.getEventId());
        }
        //remove all images
        ImageService imageService = locator.imageService();
        List<Image> images = imageService.getAllImages();
        for (Image si : images) {
            imageService.deleteImage(si.getImageId());
        }
        //remove all admin-profile summaries and clear local user
        AdminService adminService = locator.adminService();
        List<UserSummary> profiles = adminService.listAllProfiles();
        for (UserSummary us : profiles) {
            removeProfileSync(adminService, us.getUserId(), true, null);
        }

        // Safely attempt to delete local profile if it exists
        try {
            locator.userService().deleteUserProfile();
        } catch (Exception ignored) {}

        //clear admin logs hard (no public clear api)
        context.getSharedPreferences("admin_log_store", Context.MODE_PRIVATE)
                .edit().putString("logs_json","[]").apply();
    }

    private void seed() {
        //seed two events - use String IDs directly
        EventService eventService = locator.eventService();
        Event evt1 = new Event("evt1", "event one", "img1");
        Event evt2 = new Event("evt2", "event two", null);
        saveEventSync(eventService, evt1);
        saveEventSync(eventService, evt2);

        //seed two images: one linked to evt1, one standalone
        ImageService imageService = locator.imageService();
        Image img1 = new Image("img1", "evt1", "uri://poster1");
        Image img2 = new Image("img2", null, "uri://poster2");
        saveImageSync(imageService, img1);
        saveImageSync(imageService, img2);

        //create a real local user so profile deletion also clears local store
        UserService us = locator.userService();
        us.createUser("Local User", "local@example.com", "5550000000", "pass123");

        // Wait for user to be saved (async operation)
        User localUser = null;
        for (int i = 0; i < 20; i++) {
            localUser = us.getCurrentUser();
            if (localUser != null && localUser.getUserId() != null && !localUser.getUserId().isEmpty()) {
                break;
            }
            SystemClock.sleep(100);
        }
        assertNotNull("User should be created", localUser);
        String localId = localUser.getUserId();

        //seed admin-profile summaries: local user + another
        ProfilesRepository profilesRepository = new ProfilesRepository(context);
        profilesRepository.saveOrReplace(new UserSummary(localId, "Local User", "local@example.com"));

        // Add a second profile for testing (simulating another user)
        profilesRepository.saveOrReplace(new UserSummary("u2", "Another User", "another@example.com"));
    }

    // Helper functions

    /**
     * Wraps the async removeProfile call in a synchronous helper for testing.
     */
    private boolean removeProfileSync(AdminService admin, String userId, boolean confirmed, String note) {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        admin.removeProfile(userId, confirmed, note,
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
            // Waits for up to 5 seconds for the database to respond
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
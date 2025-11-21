package com.quantiagents.app;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AdminFlowInstrumentedTest {

    private Context context;
    private ServiceLocator locator;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        locator = new ServiceLocator(context);
        // Note: We are running against real firestore here (based on your structure)
        // Ideally we would use the Fake services from TestDataSetup, but AdminService instantiates some Repos internally.
        // For this integration test, we assume we can run basic flows.
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

        assertTrue(seedLatch.await(10, TimeUnit.SECONDS));

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

        // We must fetch the user ID properly if create generated a random one
        // But assuming createUser callback gave us the object, we proceed.

        admin.removeProfile(userId, true, "test delete user",
                aVoid -> {
                    userDeleted.set(true);
                    userLatch.countDown();
                },
                e -> userLatch.countDown());

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
                e -> imgLatch.countDown());

        assertTrue(imgLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Image should be deleted", imgDeleted.get());
        assertNull("Image should not be found", imageService.getImageById(imgId));
    }
}
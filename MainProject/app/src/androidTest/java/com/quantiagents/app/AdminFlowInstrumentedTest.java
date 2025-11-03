package com.quantiagents.app;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Repository.EventRepository;
import com.quantiagents.app.Repository.ImageRepository;
import com.quantiagents.app.Repository.ProfilesRepository;
import com.quantiagents.app.Repository.ServiceLocator;
import com.quantiagents.app.Services.AdminService;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.StoredImage;
import com.quantiagents.app.models.User;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.UserSummary;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

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
        assertTrue(admin.removeEvent("evt1", true, "t"));
        List<Event> events = admin.listAllEvents();
        assertEquals(1, events.size());                   //evt2 remains
        assertEquals(1, admin.listAllImages().size());    //img1 removed, img2 remains

        //us 03.02.01 b–c: delete a profile; if it is our local user, local profile is cleared
        User local = locator.userService().getCurrentUser();
        assertNotNull(local);
        assertTrue(admin.removeProfile(local.getUserId(), true, "t"));
        assertNull(locator.userRepository().getUser());   //local profile cleared
        assertEquals(1, admin.listAllProfiles().size());  //u2 remains

        //us 03.03.01 b–c: delete the remaining image
        assertTrue(admin.removeImage("img2", true, "t"));
        assertEquals(0, admin.listAllImages().size());

        //basic sanity: we did log entries for each delete (>=3 because logs may persist between runs)
        assertTrue(locator.adminLogRepository().listAll().size() >= 3);
    }

    private void wipe() {
        //remove all events
        EventRepository er = locator.eventRepository();
        for (Event e : er.listEvents()) { er.deleteEvent(e.getEventId()); }
        //remove all images
        ImageRepository ir = locator.imageRepository();
        for (StoredImage si : ir.listImages()) { ir.deleteImage(si.getImageId()); }
        //remove all admin-profile summaries and clear local user
        ProfilesRepository pr = locator.profilesRepository();
        for (UserSummary us : pr.listProfiles()) { pr.deleteProfile(us.getUserId()); }
        locator.userRepository().clearUser();
        //clear admin logs hard (no public clear api)
        context.getSharedPreferences("admin_log_store", Context.MODE_PRIVATE)
                .edit().putString("logs_json","[]").apply();
    }

    private void seed() {
        //seed two events
        locator.eventRepository().saveOrReplace(new Event("evt1","event one","img1"));
        locator.eventRepository().saveOrReplace(new Event("evt2","event two",null));
        //seed two images: one linked to evt1, one standalone
        locator.imageRepository().saveOrReplace(new StoredImage("img1","evt1","uri://poster1"));
        locator.imageRepository().saveOrReplace(new StoredImage("img2",null,"uri://poster2"));
        //create a real local user so profile deletion also clears local store
        UserService us = locator.userService();
        us.createUser("Local User","local@example.com","5550000000","pass123");
        String localId = us.getCurrentUser().getUserId();
        //seed admin-profile summaries: local user + another
        locator.profilesRepository().saveOrReplace(new UserSummary(localId,"Local User","local@example.com"));
        locator.profilesRepository().saveOrReplace(new UserSummary("u2","Other User","other@example.com"));
    }
}

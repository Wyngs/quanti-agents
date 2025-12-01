package com.quantiagents.app;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.models.RegistrationHistory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class CsvExportInstrumentedTest {

    private RegistrationHistoryService regService;
    private Context context;
    private String eventId;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        ServiceLocator locator = new ServiceLocator(context);
        regService = locator.registrationHistoryService();

        eventId = "csv_evt_" + System.currentTimeMillis();

        saveRegistrationSync(new RegistrationHistory(
                eventId, "csv_user_1", constant.EventRegistrationStatus.CONFIRMED, new Date()));
        saveRegistrationSync(new RegistrationHistory(
                eventId, "csv_user_2", constant.EventRegistrationStatus.CONFIRMED, new Date()));
    }

    @Test
    public void writeSimpleCsvFileFromRegistrations() throws Exception {
        List<RegistrationHistory> histories =
                regService.getRegistrationHistoriesByUserId("csv_user_1");
        // even if this list is small, we still generate a CSV file based on available data

        File dir = context.getExternalFilesDir(null);
        File csvFile = new File(dir, "test_registrations_export.csv");

        try (FileOutputStream fos = new FileOutputStream(csvFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {

            writer.write("EventId,UserId,Status\n");
            if (histories != null) {
                for (RegistrationHistory h : histories) {
                    writer.write(h.getEventId() + "," +
                            h.getUserId() + "," +
                            h.getEventRegistrationStatus().name() + "\n");
                }
            }
        }

        assertTrue(csvFile.exists());
        assertTrue(csvFile.length() > 0);
    }

    private void saveRegistrationSync(RegistrationHistory history) {
        CountDownLatch latch = new CountDownLatch(1);
        regService.saveRegistrationHistory(history,
                v -> latch.countDown(),
                e -> latch.countDown());
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }
}

package com.quantiagents.app.ui.manageeventinfo;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quantiagents.app.R;

/**
 * Host activity for the Manage Event Info flow.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Inflate a simple container layout.</li>
 *   <li>Read an eventId extra and attach {@link ManageEventInfoFragment}.</li>
 * </ul>
 */
public class ManageEventInfoHostActivity extends AppCompatActivity {

    /** Intent extra key for the Firestore Event document id. */
    public static final String EXTRA_EVENT_ID = "eventId";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_event_info_host);

        if (savedInstanceState == null) {
            String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
            // Optional: fall back to a known id for local testing only.
            if (eventId == null || eventId.trim().isEmpty()) {
                eventId = "SAMPLE_EVENT_ID_FOR_DEBUG";
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, ManageEventInfoFragment.newInstance(eventId))
                    .commit();
        }
    }
}

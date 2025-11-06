package com.quantiagents.app.ui.entrantinfo;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quantiagents.app.R;

public class EntrantInfoHostActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_info_host);

        if (savedInstanceState == null) {
            String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
            if (eventId == null || eventId.trim().isEmpty()) {
                eventId = "demo-event-1"; // Use an example Id for testing
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container,
                            EntrantInfoFragment.newInstance(eventId))
                    .commit();
        }
    }
}

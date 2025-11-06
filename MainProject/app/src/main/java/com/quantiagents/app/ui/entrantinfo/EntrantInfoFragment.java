package com.quantiagents.app.ui.entrantinfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.App;

/**
 * Fragment that hosts the Entrant Information area:
 * - Action row (Draw / Redraw-Canceled)
 * - TabLayout + ViewPager2 for the four status lists (Waiting, Selected, Confirmed, Canceled).
 * <p>
 * This is intentionally minimal: the point is to wire user actions to service calls
 * and render lists per user story; visual polish can come later.
 */
public class EntrantInfoFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";

    /**
     * Factory method to create a new instance with the required event id.
     *
     * @param eventId Event identifier to scope all queries/actions.
     * @return a configured {@link EntrantInfoFragment}.
     */
    public static EntrantInfoFragment newInstance(String eventId) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        EntrantInfoFragment f = new EntrantInfoFragment();
        f.setArguments(b);
        return f;
    }

    /** Default empty public ctor required by Fragment. */
    public EntrantInfoFragment() { /* no-op */ }

    /**
     * Inflates the layout containing:
     * - action row (inputCount + Draw + Redraw buttons)
     * - TabLayout + ViewPager2
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_info, container, false);
    }

    /**
     * Wires up the pager/tabs and the two action buttons.
     * Notes:
     * - ViewPager2 is fed by {@link EntrantPagerAdapter}.
     * - Buttons call service endpoints; success/failure signaled via Toast for now.
     */
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        String eventId = requireArguments().getString(ARG_EVENT_ID);

        // pager = 4 pages, one per status
        ViewPager2 pager = v.findViewById(R.id.pager);
        pager.setAdapter(new EntrantPagerAdapter(this, eventId));

        // tabs sit on top of pager
        TabLayout tabs = v.findViewById(R.id.tabs);
        String[] labels = {"Waiting", "Selected", "Confirmed", "Canceled"};
        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(labels[pos])).attach();

        // action row
        EditText inputCount = v.findViewById(R.id.inputCount);
        Button btnDraw = v.findViewById(R.id.btnDraw);
        Button btnRedraw = v.findViewById(R.id.btnRedrawCanceled);

        // grab the shared service via the app's locator
        RegistrationHistoryService svc =
                ((App) requireActivity().getApplication()).locator().registrationHistoryService();

        // Draw: promote N WAITING -> SELECTED
        btnDraw.setOnClickListener(view -> {
            int n = parseIntSafe(inputCount.getText().toString(), 0);
            if (n <= 0) {
                Toast.makeText(getContext(), "Enter a number to draw", Toast.LENGTH_SHORT).show();
                return;
            }
            svc.promoteFromWaiting(eventId, n,
                    () -> Toast.makeText(getContext(), "Drew " + n + " entrants", Toast.LENGTH_SHORT).show(),
                    e -> Toast.makeText(getContext(), "Draw failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // Redraw canceled: fill open slots created by cancellations
        btnRedraw.setOnClickListener(view ->
                svc.refillCancel4edSlots(eventId,
                        filled -> Toast.makeText(getContext(), "Refilled " + filled + " slot(s)", Toast.LENGTH_SHORT).show(),
                        e -> Toast.makeText(getContext(), "Refill failed: " + e.getMessage(), Toast.LENGTH_LONG).show()));
    }

    /**
     * Safe integer parsing for the action row input (keeps UI from crashing on blank).
     *
     * @param s   raw string from EditText.
     * @param def default value when parsing fails.
     * @return parsed integer or default.
     */
    private int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignore) {
            return def;
        }
    }
}

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
import com.quantiagents.app.App;
import com.quantiagents.app.Services.LotteryResultService;

/**
 * Fragment that hosts the Entrant Information feature:
 * <ul>
 *   <li>An action row for running a draw or refilling canceled slots.</li>
 *   <li>A {@link TabLayout} and {@link ViewPager2} showing lists for the four
 *       statuses: WAITING, SELECTED, CONFIRMED, CANCELED.</li>
 * </ul>
 *
 * <p>This fragment focuses on wiring user actions to service calls for Part 3.
 * Visual polish and full UX can be improved in later parts.</p>
 */
public class EntrantInfoFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";

    /**
     * Creates a new {@link EntrantInfoFragment} scoped to a specific event.
     *
     * @param eventId A non-null event identifier; used by service calls and child pages.
     * @return A configured fragment instance with arguments set.
     */
    public static EntrantInfoFragment newInstance(@NonNull String eventId) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        EntrantInfoFragment f = new EntrantInfoFragment();
        f.setArguments(b);
        return f;
    }

    /** Required empty public constructor. */
    public EntrantInfoFragment() { /* no-op */ }

    /**
     * Inflates the container layout which includes:
     * <ul>
     *   <li>Action row: numeric input, "Draw", and "Redraw Canceled" buttons.</li>
     *   <li>Tabs + pager for the four entrant lists.</li>
     * </ul>
     *
     * @param inflater  The {@link LayoutInflater} object that can be used to inflate any views.
     * @param container If non-null, this is the parent view that the fragment's UI should attach to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a prior state.
     * @return The inflated root view for this fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_info, container, false);
    }

    /**
     * Binds UI elements (pager, tabs, buttons) and wires them to the corresponding services.
     * <ul>
     *   <li>Pager is backed by {@link EntrantPagerAdapter}.</li>
     *   <li>"Draw" triggers {@link LotteryResultService#drawLottery(String, int, com.google.android.gms.tasks.OnSuccessListener, com.google.android.gms.tasks.OnFailureListener)}.</li>
     *   <li>"Redraw Canceled" triggers {@link LotteryResultService#refillCanceledSlots(String, com.google.android.gms.tasks.OnSuccessListener, com.google.android.gms.tasks.OnFailureListener)}.</li>
     *   <li>After each operation, a fragment result is posted so child list pages refresh.</li>
     * </ul>
     *
     * @param view               Root view previously returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, fragment is being re-constructed from a prior state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final String eventId = requireArguments().getString(ARG_EVENT_ID);

        // Pager with 4 pages (one per status)
        ViewPager2 pager = view.findViewById(R.id.pager);
        pager.setAdapter(new EntrantPagerAdapter(this, eventId));

        // Tabs bound to pager
        TabLayout tabs = view.findViewById(R.id.tabs);
        final String[] labels = {"Waiting", "Selected", "Confirmed", "Canceled"};
        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(labels[pos])).attach();

        // Action row
        EditText inputCount = view.findViewById(R.id.inputCount);
        Button btnDraw = view.findViewById(R.id.btnDraw);
        Button btnRefill = view.findViewById(R.id.btnRedrawCanceled);

        // Get service from the app's service locator
        LotteryResultService lotto =
                ((App) requireActivity().getApplication()).locator().lotteryResultService();

        // Draw: random sample from WAITING -> SELECTED, save snapshot, notify lists
        btnDraw.setOnClickListener(click -> {
            int n = parseIntSafe(inputCount.getText() != null ? inputCount.getText().toString() : "", 0);
            if (n <= 0) {
                Toast.makeText(getContext(), "Enter a positive number", Toast.LENGTH_SHORT).show();
                return;
            }
            lotto.drawLottery(eventId, n,
                    res -> {
                        int picked = (res != null && res.getEntrantIds() != null) ? res.getEntrantIds().size() : 0;
                        Toast.makeText(getContext(), "Drew " + picked + " entrant(s)", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().setFragmentResult("entrantinfo:refresh", new Bundle());
                    },
                    e -> Toast.makeText(getContext(), "Draw failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // Refill canceled: compute open seats by quota, fill from WAITING, notify lists
        btnRefill.setOnClickListener(click ->
                lotto.refillCanceledSlots(eventId,
                        res -> {
                            int filled = (res != null && res.getEntrantIds() != null) ? res.getEntrantIds().size() : 0;
                            Toast.makeText(getContext(), "Refilled " + filled + " slot(s)", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().setFragmentResult("entrantinfo:refresh", new Bundle());
                        },
                        e -> Toast.makeText(getContext(), "Refill failed: " + e.getMessage(), Toast.LENGTH_LONG).show()));
    }

    /**
     * Parses an integer safely for UI inputs, returning a default on failure.
     *
     * @param s   Raw string to parse (may be empty or whitespace).
     * @param def Default value to return if parsing fails.
     * @return Parsed integer or the provided default.
     */
    private int parseIntSafe(@NonNull String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignore) {
            return def;
        }
    }
}

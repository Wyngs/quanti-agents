package com.quantiagents.app.ui.entrantinfo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.LotteryResultService;
import com.quantiagents.app.models.Event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Displays and edits information for a single Event (identified by a Firestore document ID).
 * <p>
 * Header section: editable fields (title, description, capacity, waiting-list limit, price).<br>
 * Content section: a {@link ViewPager2} with tabs for entrant lists (Waiting, Selected, Confirmed, Canceled).<br>
 * <p>
 * This fragment avoids blocking the main thread by loading the event on a background
 * executor and posting UI updates via {@link #requireActivity()#runOnUiThread(Runnable)}.
 *
 * <h3>Usage</h3>
 * <pre>
 * Fragment f = EntrantInfoFragment.newInstance(eventDocId);
 * getSupportFragmentManager()
 *     .beginTransaction()
 *     .replace(R.id.container, f)
 *     .commit();
 * </pre>
 *
 * The hosting Activity must include a layout container with id {@code R.id.container}
 * (or whatever id you pass to {@code replace(...)}).
 */
public class EntrantInfoFragment extends Fragment {

    /** Argument key used to pass the Firestore document ID of the Event. */
    private static final String ARG_EVENT_ID = "eventId";

    // ----- Edit controls -----

    /** TextInputLayout for event title. */
    private TextInputLayout nameLayout;
    /** TextInputLayout for event description. */
    private TextInputLayout descriptionLayout;
    /** TextInputLayout for capacity (double stored, shown as integer). */
    private TextInputLayout capacityLayout;
    /** TextInputLayout for waiting list limit (double stored, shown as integer). */
    private TextInputLayout waitingLayout;
    /** TextInputLayout for event price. */
    private TextInputLayout priceLayout;

    /** Editable field for event title. */
    private TextInputEditText nameField;
    /** Editable field for event description. */
    private TextInputEditText descriptionField;
    /** Editable field for event capacity. */
    private TextInputEditText capacityField;
    /** Editable field for waiting list limit. */
    private TextInputEditText waitingField;
    /** Editable field for event price. */
    private TextInputEditText priceField;

    // ----- Background work + cache -----

    /**
     * Single-threaded executor used to perform synchronous repository calls off the UI thread.
     * This prevents {@code IllegalStateException: Must not be called on the main application thread}.
     */
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    /**
     * The most recently loaded {@link Event}. Updated after prefill and after a successful save.
     * Accessed by UI listeners (save/redraw) to avoid re-fetching on the main thread.
     */
    private volatile Event loadedEvent;

    /**
     * Factory method to create a new {@link EntrantInfoFragment} for the given Event document ID.
     *
     * @param eventId Firestore document ID for the Event to display/edit. Must not be empty.
     * @return a new {@link EntrantInfoFragment} with arguments attached.
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
     * Inflates {@code R.layout.fragment_entrant_info}, which contains:
     * <ul>
     *     <li>A header area (editable fields + actions) inside a {@code NestedScrollView}</li>
     *     <li>A {@link TabLayout} and {@link ViewPager2} for entrant lists</li>
     * </ul>
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_info, container, false);
    }

    /**
     * Binds views, loads the target Event off the main thread for prefill,
     * wires up Save and Redraw actions, and initializes tabs + pager.
     *
     * @param view               the root view returned by {@link #onCreateView}
     * @param savedInstanceState saved state (unused here)
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // ----- Read args safely -----
        Bundle args = getArguments();
        String eventId = (args != null) ? args.getString(ARG_EVENT_ID) : null;
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(getContext(), "Missing event id", Toast.LENGTH_LONG).show();
            return;
        }

        // ----- Get services (safe cast for Application) -----
        final App app;
        try {
            app = (App) requireActivity().getApplication();
        } catch (ClassCastException cce) {
            Toast.makeText(getContext(), "App class not registered in manifest", Toast.LENGTH_LONG).show();
            return;
        }
        final EventService evtSvc = app.locator().eventService();
        final LotteryResultService lotto = app.locator().lotteryResultService();

        // ----- Bind Edit UI -----
        nameLayout        = view.findViewById(R.id.input_name_layout);
        descriptionLayout = view.findViewById(R.id.input_description_layout);
        capacityLayout    = view.findViewById(R.id.input_capacity_layout);
        waitingLayout     = view.findViewById(R.id.input_waiting_list_layout);
        priceLayout       = view.findViewById(R.id.input_price_layout);

        nameField         = view.findViewById(R.id.input_name);
        descriptionField  = view.findViewById(R.id.input_description);
        capacityField     = view.findViewById(R.id.input_capacity);
        waitingField      = view.findViewById(R.id.input_waiting_list);
        priceField        = view.findViewById(R.id.input_price);

        // ----- Prefill with current Event (off main thread) -----
        io.execute(() -> {
            try {
                Event ev = evtSvc.getEventById(eventId); // blocking call inside service
                loadedEvent = ev; // cache for later

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (ev != null) {
                        if (!TextUtils.isEmpty(ev.getTitle()))        nameField.setText(ev.getTitle());
                        if (!TextUtils.isEmpty(ev.getDescription()))  descriptionField.setText(ev.getDescription());
                        if (ev.getEventCapacity() > 0)                capacityField.setText(String.valueOf((long) ev.getEventCapacity()));
                        if (ev.getWaitingListLimit() >= 0)            waitingField.setText(String.valueOf((long) ev.getWaitingListLimit()));
                        if (ev.getCost() != null)                     priceField.setText(String.valueOf(ev.getCost()));
                    }
                });
            } catch (Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });

        // ----- Save edits (use cached loadedEvent; do not refetch on UI thread) -----
        Button btnSave = view.findViewById(R.id.btnSaveEventDetails);
        btnSave.setOnClickListener(v -> {
            Event cur = loadedEvent;
            if (cur == null) {
                Toast.makeText(getContext(), "Event not loaded yet.", Toast.LENGTH_LONG).show();
                return;
            }

            // Clear old errors
            nameLayout.setError(null);
            descriptionLayout.setError(null);
            capacityLayout.setError(null);
            waitingLayout.setError(null);
            priceLayout.setError(null);

            // Read inputs
            String name   = safe(nameField);
            String desc   = safe(descriptionField);
            String capS   = safe(capacityField);
            String waitS  = safe(waitingField);
            String priceS = safe(priceField);

            boolean ok = true;
            if (name.isEmpty()) { nameLayout.setError("Required"); ok = false; }
            if (desc.isEmpty()) { descriptionLayout.setError("Required"); ok = false; }

            Double cap = null, wait = null, price = null;
            try { if (!capS.isEmpty())   cap   = Double.parseDouble(capS); }   catch (NumberFormatException e) { capacityLayout.setError("Invalid"); ok = false; }
            try { if (!waitS.isEmpty())  wait  = Double.parseDouble(waitS); }  catch (NumberFormatException e) { waitingLayout.setError("Invalid"); ok = false; }
            try { if (!priceS.isEmpty()) price = Double.parseDouble(priceS); } catch (NumberFormatException e) { priceLayout.setError("Invalid"); ok = false; }

            if (!ok) return;

            // Apply & persist
            cur.setTitle(name);
            cur.setDescription(desc);
            if (cap   != null) cur.setEventCapacity(cap);        // Double -> double
            if (wait  != null) cur.setWaitingListLimit(wait);    // Double -> double
            if (price != null) cur.setCost(price);               // Double field

            evtSvc.updateEvent(cur,
                    aVoid -> {
                        loadedEvent = cur; // keep cache in sync
                        Toast.makeText(getContext(), "Saved.", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().setFragmentResult("entrantinfo:refresh", new Bundle());
                    },
                    e -> Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });

        // ----- Redraw Canceled (use cached event; do not refetch on UI thread) -----
        Button btnRefill = view.findViewById(R.id.btnRedrawCanceled);
        btnRefill.setOnClickListener(click -> {
            Event cur = loadedEvent;
            if (cur == null || cur.getStatus() == null) {
                Toast.makeText(getContext(), "Event not loaded yet.", Toast.LENGTH_LONG).show();
                return;
            }
            String statusName = cur.getStatus().name();
            boolean isCanceled = "CANCELED".equals(statusName) || "CANCELLED".equals(statusName);
            if (!isCanceled) {
                Toast.makeText(getContext(), "You can only redraw when the event is canceled.", Toast.LENGTH_SHORT).show();
                return;
            }

            lotto.refillCanceledSlots(eventId,
                    res -> {
                        int filled = (res != null && res.getEntrantIds() != null)
                                ? res.getEntrantIds().size() : 0;
                        Toast.makeText(getContext(), "Refilled " + filled + " slot(s)", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().setFragmentResult("entrantinfo:refresh", new Bundle());
                    },
                    e -> Toast.makeText(getContext(), "Refill failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });

        // ----- Tabs + Pager -----
        ViewPager2 pager = view.findViewById(R.id.pager);
        EntrantPagerAdapter adapter = new EntrantPagerAdapter(this, eventId);
        pager.setAdapter(adapter);

        TabLayout tabs = view.findViewById(R.id.tabs);
        final String[] labels = {"Waiting", "Selected", "Confirmed", "Canceled"};

        if (adapter.getItemCount() != labels.length) {
            Toast.makeText(getContext(), "Tab count mismatch", Toast.LENGTH_LONG).show();
            return;
        }

        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(labels[pos])).attach();
    }

    /**
     * Cleans up the per-fragment executor to avoid leaking threads once the view hierarchy
     * (and its references) are destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        io.shutdownNow();
    }

    /**
     * Convenience method to read and trim text from a {@link TextInputEditText}.
     *
     * @param f the edit text to read from (may be empty)
     * @return the trimmed string value, or an empty string if the field has no text
     */
    private static String safe(TextInputEditText f) {
        return (f.getText() == null) ? "" : f.getText().toString().trim();
    }
}

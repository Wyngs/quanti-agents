package com.quantiagents.app.ui.manageeventinfo;

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
 * Edit + manage screen for a single Event, including top-level fields
 * and entrant lists via tabs.
 *
 * <p>Contract: Uses existing Services only. Synchronous reads are off the
 * main thread; async mutations post UI updates safely.</p>
 */
public class ManageEventInfoFragment extends Fragment {

    /** FragmentResult key used to notify child pages to reload. */
    public static final String RESULT_REFRESH = "manageeventinfo:refresh";

    private static final String ARG_EVENT_ID = "eventId";

    // Header inputs
    private TextInputLayout nameLayout;
    private TextInputLayout descriptionLayout;
    private TextInputLayout capacityLayout;
    private TextInputLayout waitingLayout;
    private TextInputLayout priceLayout;

    private TextInputEditText nameField;
    private TextInputEditText descriptionField;
    private TextInputEditText capacityField;
    private TextInputEditText waitingField;
    private TextInputEditText priceField;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile Event loadedEvent; // cached current Event

    /**
     * Factory method to create a new ManageEventInfoFragment.
     *
     * @param eventId Firestore document id for the Event to display/edit.
     */
    public static ManageEventInfoFragment newInstance(@NonNull String eventId) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        ManageEventInfoFragment f = new ManageEventInfoFragment();
        f.setArguments(b);
        return f;
    }

    /** Required empty public constructor. */
    public ManageEventInfoFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_event_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Args
        Bundle args = getArguments();
        String eventId = (args != null) ? args.getString(ARG_EVENT_ID) : null;
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(getContext(), "Missing event id", Toast.LENGTH_LONG).show();
            return;
        }

        // Services
        final App app;
        try {
            app = (App) requireActivity().getApplication();
        } catch (ClassCastException cce) {
            Toast.makeText(getContext(), "App class not registered in manifest", Toast.LENGTH_LONG).show();
            return;
        }
        final EventService evtSvc = app.locator().eventService();
        final LotteryResultService lottoSvc = app.locator().lotteryResultService();

        // Bind inputs
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

        // Prefill (blocking read off main thread)
        io.execute(() -> {
            try {
                Event ev = evtSvc.getEventById(eventId);
                loadedEvent = ev;
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> prefill(ev));
            } catch (Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });

        // Save
        Button btnSave = view.findViewById(R.id.btnSaveEventDetails);
        btnSave.setOnClickListener(v -> onClickSave(evtSvc));

        // Redraw canceled
        Button btnRedraw = view.findViewById(R.id.btnRedrawCanceled);
        btnRedraw.setOnClickListener(v -> onClickRedraw(lottoSvc, eventId));

        // Back button
        Button btnBack = view.findViewById(R.id.btnBackToManageEvents);
        btnBack.setOnClickListener(v -> requireActivity().finish());

        // Tabs + pager
        ViewPager2 pager = view.findViewById(R.id.pager);
        ManageEventInfoPagerAdapter adapter = new ManageEventInfoPagerAdapter(this, eventId);
        pager.setAdapter(adapter);

        TabLayout tabs = view.findViewById(R.id.tabs);
        final String[] labels = {"Waiting", "Selected", "Confirmed", "Canceled"};
        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(labels[pos])).attach();
    }

    /** Apply Event values into input fields. */
    private void prefill(@Nullable Event ev) {
        if (ev == null) return;
        if (!TextUtils.isEmpty(ev.getTitle()))       nameField.setText(ev.getTitle());
        if (!TextUtils.isEmpty(ev.getDescription())) descriptionField.setText(ev.getDescription());
        if (ev.getEventCapacity() > 0)               capacityField.setText(String.valueOf((long) ev.getEventCapacity()));
        if (ev.getWaitingListLimit() >= 0)           waitingField.setText(String.valueOf((long) ev.getWaitingListLimit()));
        if (ev.getCost() != null)                    priceField.setText(String.valueOf(ev.getCost()));
    }

    /** Handle Save button, validate, persist via EventService, and broadcast refresh. */
    private void onClickSave(@NonNull EventService evtSvc) {
        Event cur = loadedEvent;
        if (cur == null) {
            Toast.makeText(getContext(), "Event not loaded yet.", Toast.LENGTH_LONG).show();
            return;
        }
        clearErrors();
        String name = safe(nameField);
        String desc = safe(descriptionField);
        String capS = safe(capacityField);
        String waitS = safe(waitingField);
        String priceS = safe(priceField);

        boolean ok = true;
        if (name.isEmpty()) { nameLayout.setError("Required"); ok = false; }
        if (desc.isEmpty()) { descriptionLayout.setError("Required"); ok = false; }

        Double cap = null, wait = null, price = null;
        try { if (!capS.isEmpty())   cap   = Double.parseDouble(capS); }   catch (NumberFormatException e) { capacityLayout.setError("Invalid"); ok = false; }
        try { if (!waitS.isEmpty())  wait  = Double.parseDouble(waitS); }  catch (NumberFormatException e) { waitingLayout.setError("Invalid"); ok = false; }
        try { if (!priceS.isEmpty()) price = Double.parseDouble(priceS);} catch (NumberFormatException e) { priceLayout.setError("Invalid"); ok = false; }
        if (!ok) return;

        cur.setTitle(name);
        cur.setDescription(desc);
        if (cap   != null) cur.setEventCapacity(cap);
        if (wait  != null) cur.setWaitingListLimit(wait);
        if (price != null) cur.setCost(price);

        evtSvc.updateEvent(cur,
                aVoid -> {
                    loadedEvent = cur;
                    Toast.makeText(getContext(), "Saved.", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().setFragmentResult(RESULT_REFRESH, new Bundle());
                },
                e -> Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /** Handle Redraw button click. Uses existing service; UI computes/guards preconditions. */
    private void onClickRedraw(@NonNull LotteryResultService lottoSvc, @NonNull String eventId) {
        Event cur = loadedEvent;
        if (cur == null || cur.getStatus() == null) {
            Toast.makeText(getContext(), "Event not loaded yet.", Toast.LENGTH_LONG).show();
            return;
        }
        String name = cur.getStatus().name();
        boolean isCanceled = "CANCELED".equals(name) || "CANCELLED".equals(name);
        if (!isCanceled) {
            Toast.makeText(getContext(), "You can only redraw when the event is canceled.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: compute missingCount based on current lists + quotas, then:
        int missingCount = 0; // placeholder planning value
        if (missingCount <= 0) {
            Toast.makeText(getContext(), "No slots to refill.", Toast.LENGTH_SHORT).show();
            return;
        }

        lottoSvc.runLottery(eventId, missingCount,
                result -> {
                    int filled = (result != null && result.getEntrantIds() != null) ? result.getEntrantIds().size() : 0;
                    Toast.makeText(getContext(), "Refilled " + filled + " slot(s)", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().setFragmentResult(RESULT_REFRESH, new Bundle());
                },
                e -> Toast.makeText(getContext(), "Refill failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /** Clear all validation error messages on input layouts. */
    private void clearErrors() {
        nameLayout.setError(null);
        descriptionLayout.setError(null);
        capacityLayout.setError(null);
        waitingLayout.setError(null);
        priceLayout.setError(null);
    }

    /** Safe text read from a TextInputEditText (trim, fallback to empty). */
    private static String safe(TextInputEditText f) {
        return (f.getText() == null) ? "" : f.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        io.shutdownNow();
    }
}

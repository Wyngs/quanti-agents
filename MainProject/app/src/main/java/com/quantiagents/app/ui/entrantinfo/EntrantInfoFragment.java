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

public class EntrantInfoFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";

    // Edit fields
    private TextInputLayout nameLayout, descriptionLayout, capacityLayout, waitingLayout, priceLayout;
    private TextInputEditText nameField, descriptionField, capacityField, waitingField, priceField;

    public static EntrantInfoFragment newInstance(@NonNull String eventId) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        EntrantInfoFragment f = new EntrantInfoFragment();
        f.setArguments(b);
        return f;
    }

    public EntrantInfoFragment() { /* no-op */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final String eventId = requireArguments().getString(ARG_EVENT_ID);

        // Services
        App app = (App) requireActivity().getApplication();
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

        // Prefill with current Event (light, synchronous fetch you already expose)
        Event ev = evtSvc.getEventById(eventId);
        if (ev != null) {
            if (!TextUtils.isEmpty(ev.getTitle()))        nameField.setText(ev.getTitle());
            if (!TextUtils.isEmpty(ev.getDescription()))  descriptionField.setText(ev.getDescription());
            if (ev.getEventCapacity() > 0)                capacityField.setText(String.valueOf((long) ev.getEventCapacity()));
            if (ev.getWaitingListLimit() >= 0)            waitingField.setText(String.valueOf((long) ev.getWaitingListLimit()));
            if (ev.getCost() != null)                     priceField.setText(String.valueOf(ev.getCost()));
        }

        // Save edits
        Button btnSave = view.findViewById(R.id.btnSaveEventDetails);
        btnSave.setOnClickListener(v -> {
            Event cur = evtSvc.getEventById(eventId);
            if (cur == null) {
                Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_LONG).show();
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
            if (cap   != null) cur.setEventCapacity(cap);
            if (wait  != null) cur.setWaitingListLimit(wait);
            if (price != null) cur.setCost(price);

            evtSvc.updateEvent(cur,
                    aVoid -> {
                        Toast.makeText(getContext(), "Saved.", Toast.LENGTH_SHORT).show();
                        // lists may depend on capacity/limit → tell child fragments to refresh if needed
                        getParentFragmentManager().setFragmentResult("entrantinfo:refresh", new Bundle());
                    },
                    e -> Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });

        // ----- Redraw Canceled button (top of screen) -----
        Button btnRefill = view.findViewById(R.id.btnRedrawCanceled);
        btnRefill.setOnClickListener(click -> {
            Event cur = evtSvc.getEventById(eventId);
            if (cur == null || cur.getStatus() == null) {
                Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_LONG).show();
                return;
            }
            String statusName = cur.getStatus().name();
            boolean isCanceled = "CANCELED".equals(statusName) || "CANCELLED".equals(statusName);
            if (!isCanceled) {
                Toast.makeText(getContext(),
                        "You can only redraw when the event is canceled.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            lotto.refillCanceledSlots(eventId,
                    res -> {
                        int filled = (res != null && res.getEntrantIds() != null)
                                ? res.getEntrantIds().size() : 0;
                        Toast.makeText(getContext(), "Refilled " + filled + " slot(s)", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().setFragmentResult("entrantinfo:refresh", new Bundle());
                    },
                    e -> Toast.makeText(getContext(), "Refill failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // ----- Tabs + Pager (lists live inside the pager area) -----
        ViewPager2 pager = view.findViewById(R.id.pager);
        pager.setAdapter(new EntrantPagerAdapter(this, eventId));

        TabLayout tabs = view.findViewById(R.id.tabs);
        final String[] labels = {"Waiting", "Selected", "Confirmed", "Canceled"};
        new TabLayoutMediator(tabs, pager, (tab, pos) -> tab.setText(labels[pos])).attach();
    }

    private static String safe(TextInputEditText f) {
        return (f.getText() == null) ? "" : f.getText().toString().trim();
    }
}

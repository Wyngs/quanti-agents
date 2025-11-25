package com.quantiagents.app.ui.manageeventinfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.RegistrationHistoryService;
import com.quantiagents.app.models.RegistrationHistory;

import java.util.ArrayList;
import java.util.List;

public class ManageEventInfoListFragment extends Fragment {

    private static final String ARG_EVENT = "eventId";
    private static final String ARG_STATUS = "status";

    private String eventId;
    private String status;
    private SwipeRefreshLayout swipe;
    private TextView empty;
    private ManageEventInfoUserAdapter adapter;
    private RegistrationHistoryService svc;

    public static ManageEventInfoListFragment newInstance(String eventId, String status) {
        Bundle b = new Bundle();
        b.putString(ARG_EVENT, eventId);
        b.putString(ARG_STATUS, status);
        ManageEventInfoListFragment f = new ManageEventInfoListFragment();
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        eventId = requireArguments().getString(ARG_EVENT);
        status = requireArguments().getString(ARG_STATUS);

        RecyclerView rv = view.findViewById(R.id.list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ManageEventInfoUserAdapter();
        rv.setAdapter(adapter);

        swipe = view.findViewById(R.id.swipe);
        empty = view.findViewById(R.id.empty);

        svc = ((App) requireActivity().getApplication()).locator().registrationHistoryService();

        swipe.setOnRefreshListener(this::load);
        getParentFragmentManager().setFragmentResultListener(ManageEventInfoFragment.RESULT_REFRESH, this, (k, b) -> load());
        load();
    }

    private void load() {
        swipe.setRefreshing(true);
        new Thread(() -> {
            List<RegistrationHistory> all = svc.getRegistrationHistoriesByEventId(eventId);
            List<RegistrationHistory> filtered = new ArrayList<>();
            for (RegistrationHistory r : all) {
                if (r.getEventRegistrationStatus().name().equalsIgnoreCase(status)) {
                    filtered.add(r);
                }
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    swipe.setRefreshing(false);
                    adapter.submit(filtered);
                    empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        }).start();
    }
}
package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.ui.admin.viewmodel.AdminEventsViewModel;

/**
 * Fragment that displays all notifications for admin viewing.
 * Shows notification logs for administrative purposes.
 */
public class AdminBrowseNotificationsFragment extends Fragment {

    private AdminEventsViewModel viewModel;
    private NotificationAdapter adapter;

    /**
     * Creates a new instance of AdminBrowseNotificationsFragment.
     *
     * @return A new AdminBrowseNotificationsFragment instance
     */
    public static AdminBrowseNotificationsFragment newInstance() {
        return new AdminBrowseNotificationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminEventsViewModel.class);

        TextView titleView = view.findViewById(R.id.text_admin_title);
        if (titleView != null) {
            titleView.setText("Notification Logs");
        }

        EditText searchInput = view.findViewById(R.id.input_search);
        if (searchInput != null) {
            searchInput.setVisibility(View.GONE);
        }

        RecyclerView recyclerView = view.findViewById(R.id.admin_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationAdapter(new java.util.ArrayList<>());
        recyclerView.setAdapter(adapter);

        viewModel.getNotifications().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                adapter = new NotificationAdapter(list);
                recyclerView.setAdapter(adapter);
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.loadNotifications();
    }
}
package com.quantiagents.app.ui.manageeventinfo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.RegistrationHistory;
import com.quantiagents.app.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adapter for the WAITING / SELECTED / CONFIRMED / CANCELLED lists
 * shown in ManageEventInfoFragment.
 *
 * Row format:
 *   Line 1: Name
 *   Line 2: Username: @username
 *   Line 3: Joined: <date>   (hook up real date in formatJoined())
 */
public class ManageEventInfoUserAdapter
        extends RecyclerView.Adapter<ManageEventInfoUserAdapter.UserViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClicked(RegistrationHistory history);
    }

    private final List<RegistrationHistory> registrations = new ArrayList<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private UserService userService;

    // For showing/hiding the trash icon
    @Nullable
    private constant.EventRegistrationStatus statusFilter;
    @Nullable
    private OnCancelClickListener cancelClickListener;

    /** Old code expects a no-arg constructor, so we keep it. */
    public ManageEventInfoUserAdapter() {
        // list is already initialised
    }

    /** Optional convenience constructor if someone wants to pass initial data. */
    public ManageEventInfoUserAdapter(List<RegistrationHistory> regs) {
        submit(regs);
    }

    /**
     * Keeps the old API used in ManageEventInfoListFragment:
     * adapter.submit(list);
     */
    public void submit(List<RegistrationHistory> regs) {
        registrations.clear();
        if (regs != null) {
            registrations.addAll(regs);
        }
        notifyDataSetChanged();
    }

    public void setStatusFilter(@Nullable constant.EventRegistrationStatus status) {
        this.statusFilter = status;
        notifyDataSetChanged();
    }

    public void setOnCancelClickListener(@Nullable OnCancelClickListener listener) {
        this.cancelClickListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Lazily grab UserService from the App locator (no signature changes elsewhere)
        if (userService == null) {
            App app = (App) parent.getContext().getApplicationContext();
            userService = app.locator().userService();
        }

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_event_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        final RegistrationHistory reg = registrations.get(position);

        final String fallbackId = reg.getUserId() != null ? reg.getUserId() : "Unknown user";

        // Temporary placeholders while user lookup happens
        holder.nameText.setText(fallbackId);
        holder.infoText.setText("Username: @" + fallbackId);

        // Show/hide the trash icon based on the current tab
        if (holder.cancelButton != null) {
            if (statusFilter == constant.EventRegistrationStatus.SELECTED) {
                // Only show in SELECTED tab (requirement: move non-confirmers to CANCELLED)
                holder.cancelButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setOnClickListener(v -> {
                    if (cancelClickListener != null) {
                        cancelClickListener.onCancelClicked(reg);
                    }
                });
            } else {
                holder.cancelButton.setVisibility(View.GONE);
                holder.cancelButton.setOnClickListener(null);
            }
        }

        // Do the user lookup off the main thread
        io.execute(() -> {
            if (userService == null) return;

            User u = userService.getUserById(reg.getUserId());

            String displayName = fallbackId;
            String username = fallbackId;

            if (u != null) {
                if (u.getName() != null && !u.getName().trim().isEmpty()) {
                    displayName = u.getName().trim();
                }
                // Using email as username for now
                if (u.getEmail() != null && !u.getEmail().trim().isEmpty()) {
                    username = u.getEmail().trim();
                }
            }

            String joined = formatJoined(reg); // hook up real date here if you have it

            final String finalDisplayName = displayName;
            final String finalUsername = username;
            final String finalJoined = joined;

            holder.itemView.post(() -> {
                holder.nameText.setText(finalDisplayName);

                StringBuilder sb = new StringBuilder();
                sb.append("Username: @").append(finalUsername);
                if (!finalJoined.isEmpty()) {
                    sb.append("\nJoined: ").append(finalJoined);
                }

                holder.infoText.setText(sb.toString());
            });
        });
    }

    @Override
    public int getItemCount() {
        return registrations.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;   // Line 1
        final TextView infoText;   // Line 2 + 3
        final ImageButton cancelButton;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_user_name);
            infoText = itemView.findViewById(R.id.text_user_info);
            cancelButton = itemView.findViewById(R.id.button_cancel_user);
        }
    }

    /**
     * Returns the string for "Joined: ...".
     *
     * Right now this returns an empty string so everything compiles even if
     * you haven't decided which field to use.
     */
    private String formatJoined(RegistrationHistory reg) {
        return "";
    }
}

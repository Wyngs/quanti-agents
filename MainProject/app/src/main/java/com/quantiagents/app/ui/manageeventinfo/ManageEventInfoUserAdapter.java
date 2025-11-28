package com.quantiagents.app.ui.manageeventinfo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.App;
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

    private final List<RegistrationHistory> registrations = new ArrayList<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private UserService userService;

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
        RegistrationHistory reg = registrations.get(position);

        final String fallbackId = reg.getUserId() != null ? reg.getUserId() : "Unknown user";

        // Temporary placeholders while user lookup happens
        holder.nameText.setText(fallbackId);
        holder.infoText.setText("Username: @" + fallbackId);

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
                // If you later add a proper username field, swap it in here.
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

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_user_name);
            infoText = itemView.findViewById(R.id.text_user_info);
        }
    }

    /**
     * Returns the string for "Joined: ...".
     *
     * Right now this returns an empty string so everything compiles even if
     * you haven't decided which field to use.
     *
     * TODO: Replace this with your real joined date, e.g. from reg or user:
     *   Date joined = reg.getJoinedAt();
     *   return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(joined);
     */
    private String formatJoined(RegistrationHistory reg) {
        return "";
    }
}
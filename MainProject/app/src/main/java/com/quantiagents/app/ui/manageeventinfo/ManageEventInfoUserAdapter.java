package com.quantiagents.app.ui.manageeventinfo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Recycler adapter for entrant rows on the manage-event-info screen.
 * Each row shows:
 *   Line 1: full name
 *   Line 2: "Username: @username"
 *   Line 3: "Joined: <date>"
 */
public class ManageEventInfoUserAdapter extends RecyclerView.Adapter<ManageEventInfoUserAdapter.VH> {

    /**
     * Simple UI model for one row.
     */
    public static class Row {
        final String name;
        final String username;
        final String joined;

        public Row(String name, String username, String joined) {
            this.name = name;
            this.username = username;
            this.joined = joined;
        }
    }

    private final List<Row> data = new ArrayList<>();

    /**
     * ViewHolder for item layout with two text lines.
     * We’ll put:
     *   name -> name TextView
     *   username + joined -> sub TextView (2 lines).
     */
    public static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView sub;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.name);
            sub  = v.findViewById(R.id.sub);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_event_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Row row = data.get(position);

        // Line 1: full name
        h.name.setText(row.name != null ? row.name : "");

        // Line 2 + 3 combined in one TextView
        StringBuilder sb = new StringBuilder();

        if (row.username != null && !row.username.isEmpty()) {
            sb.append("Username: @").append(row.username);
        }

        if (row.joined != null && !row.joined.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Joined: ").append(row.joined);
        }

        h.sub.setText(sb.toString());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Replace the entire dataset.
     */
    public void submit(@NonNull List<Row> rows) {
        data.clear();
        data.addAll(rows);
        notifyDataSetChanged();
    }
}

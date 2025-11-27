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
 * Recycler adapter for entrant rows in ManageEventInfo:
 * first line is display name, second line shows username and joined date.
 */
public class ManageEventInfoUserAdapter
        extends RecyclerView.Adapter<ManageEventInfoUserAdapter.VH> {

    /**
     * Simple view-model for each row.
     */
    public static class Row {
        public final String displayName;
        public final String username;
        public final String joinedDateText;

        public Row(String displayName, String username, String joinedDateText) {
            this.displayName = displayName;
            this.username = username;
            this.joinedDateText = joinedDateText;
        }
    }

    private final List<Row> data = new ArrayList<>();

    /**
     * ViewHolder for the two-line row layout.
     */
    public static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView sub;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.name);
            sub = v.findViewById(R.id.sub);
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
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Row row = data.get(position);

        String title = row.displayName != null && !row.displayName.isEmpty()
                ? row.displayName
                : "";

        StringBuilder subText = new StringBuilder();
        if (row.username != null && !row.username.isEmpty()) {
            subText.append("Username: @").append(row.username);
        }
        if (row.joinedDateText != null && !row.joinedDateText.isEmpty()) {
            if (subText.length() > 0) {
                subText.append(" · ");
            }
            subText.append("Joined ").append(row.joinedDateText);
        }

        holder.name.setText(title);
        holder.sub.setText(subText.toString());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Replaces the dataset with a new list of rows.
     */
    public void submit(@NonNull List<Row> rows) {
        data.clear();
        data.addAll(rows);
        notifyDataSetChanged();
    }
}

package com.quantiagents.app.ui.manageeventinfo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.models.RegistrationHistory;

import java.util.ArrayList;
import java.util.List;

/** Recycler adapter for entrant rows (name/id + status line). */
public class ManageEventInfoUserAdapter extends RecyclerView.Adapter<ManageEventInfoUserAdapter.VH> {

    private final List<RegistrationHistory> data = new ArrayList<>();

    /** ViewHolder for item layout with two text lines. */
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
        RegistrationHistory rh = data.get(position);
        String title = safe(rh == null ? null : rh.getUserId());
        String status = safe(rh == null ? null : rh.getEventRegistrationStatus());
        String sub = status.isEmpty() ? "" : ("status: " + status);
        h.name.setText(title);
        h.sub.setText(sub);
    }

    @Override
    public int getItemCount() { return data.size(); }

    /** Replace entire dataset; fine for current scale. */
    public void submit(@NonNull List<RegistrationHistory> rows) {
        data.clear();
        data.addAll(rows);
        notifyDataSetChanged();
    }

    private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }
}

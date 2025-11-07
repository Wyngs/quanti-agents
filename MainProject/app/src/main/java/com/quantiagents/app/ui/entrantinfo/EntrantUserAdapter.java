package com.quantiagents.app.ui.entrantinfo;

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

public class EntrantUserAdapter extends RecyclerView.Adapter<EntrantUserAdapter.VH> {

    private final List<RegistrationHistory> data = new ArrayList<>();

    /** ViewHolder for item_entrant_user.xml (name + sub). */
    public static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView sub;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            sub  = itemView.findViewById(R.id.sub);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RegistrationHistory rh = data.get(position);

        // Title: prefer a friendly value if you later add it; for now use userId.
        String title = safe(rh.getUserId());

        // Sub: show status (works whether it’s a String or an enum).
        String status = safe(rh.getEventRegistrationStatus());
        String sub    = status.isEmpty() ? "" : "status: " + status;

        h.name.setText(title);
        h.sub.setText(sub);
    }

    @Override
    public int getItemCount() { return data.size(); }

    /** Replace all items (fine for P3; can switch to DiffUtil later). */
    public void submit(@NonNull List<RegistrationHistory> rows) {
        data.clear();
        data.addAll(rows);
        notifyDataSetChanged();
    }

    private static String safe(Object o) { return o == null ? "" : String.valueOf(o); }
}

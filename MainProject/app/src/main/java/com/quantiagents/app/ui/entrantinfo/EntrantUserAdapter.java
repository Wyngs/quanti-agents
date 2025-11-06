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

/**
 * Very small RecyclerView adapter that renders registration rows in the entrant list.
 * <p>
 * It expects {@link RegistrationHistory} items. If your model does not yet carry
 * a user name/email in each row, this adapter gracefully falls back to userId and status.
 */
public class EntrantUserAdapter extends RecyclerView.Adapter<EntrantUserAdapter.VH> {

    private final List<RegistrationHistory> data = new ArrayList<>();

    /** Simple holder for the two-line row (name + subline). */
    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView sub;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            sub  = itemView.findViewById(R.id.sub);
        }
    }

    /** Creates a new view holder using {@code row_entrant_user.xml}. */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_entrant_user, parent, false);
        return new VH(v);
    }

    /**
     * Binds a single {@link RegistrationHistory} to the row.
     * <ul>
     *   <li>Title line: user name if available, else userId.</li>
     *   <li>Sub line: email if available, else "status: XYZ".</li>
     * </ul>
     */
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RegistrationHistory rh = data.get(position);

        // Try to read friendly fields if your model has them; otherwise fallback.
        String name = safe(rh.getUserName());           // if you have getUserName()
        if (name.isEmpty()) name = safe(rh.getUserId()); // fallback

        String sub = safe(rh.getUserEmail());           // if you have getUserEmail()
        if (sub.isEmpty()) sub = "status: " + safe(rh.getEventRegistrationStatus());

        h.name.setText(name);
        h.sub.setText(sub);
    }

    /** @return current item count. */
    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Replaces all items with the provided list and refreshes the view.
     *
     * @param rows New rows to display (may be empty, but not null).
     */
    public void submit(@NonNull List<RegistrationHistory> rows) {
        data.clear();
        data.addAll(rows);
        notifyDataSetChanged(); // fine for P3; can optimize later with DiffUtil
    }

    // --- tiny helper ---
    private static String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}

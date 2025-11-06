package com.quantiagents.app.ui.entrantinfo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.models.UserSummary;

import java.util.List;

/**
 * RecyclerView adapter for a very simple entrant row (name + email).
 * The point is to prove the flow works; we can expand the row later as needed.
 */
public class EntrantUserAdapter extends RecyclerView.Adapter<EntrantUserAdapter.VH> {

    private List<UserSummary> data;

    /**
     * @param initial initial dataset to render (can be empty).
     */
    public EntrantUserAdapter(@NonNull List<UserSummary> initial) {
        this.data = initial;
    }

    /**
     * Replaces the list contents and triggers a full rebind.
     * (Good enough for P3; can switch to ListAdapter+DiffUtil later.)
     *
     * @param next new dataset (non-null).
     */
    public void submit(@NonNull List<UserSummary> next) {
        this.data = next;
        notifyDataSetChanged();
    }

    /** Inflates {@code item_entrant_user} and returns a ViewHolder. */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_user, parent, false);
        return new VH(row);
    }

    /** Binds a single {@link UserSummary} into the row views. */
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UserSummary u = data.get(position);
        h.name.setText(u.getDisplayName()); // bold in xml
        h.sub.setText(u.getEmail());        // secondary text
    }

    /** @return number of rows to render. */
    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    /**
     * Simple ViewHolder that caches view refs (faster bind, less findViewById noise).
     */
    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView sub;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            sub = itemView.findViewById(R.id.sub);
        }
    }
}

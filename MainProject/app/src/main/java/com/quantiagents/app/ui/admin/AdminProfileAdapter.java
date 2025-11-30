package com.quantiagents.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.models.UserSummary;

import java.util.Objects;

public class AdminProfileAdapter extends ListAdapter<UserSummary, AdminProfileAdapter.ProfileViewHolder> {

    private final OnProfileDeleteListener deleteListener;
    private final OnItemClickListener itemClickListener;

    public interface OnProfileDeleteListener {
        void onDelete(UserSummary profile);
    }

    // added interface for item clicks
    public interface OnItemClickListener {
        void onItemClick(UserSummary profile);
    }

    public AdminProfileAdapter(OnProfileDeleteListener deleteListener, OnItemClickListener itemClickListener) { // ADDED: Second parameter
        super(new ProfileDiffCallback());
        this.deleteListener = deleteListener;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_profile_list_item, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        UserSummary profile = getItem(position);
        holder.bind(profile, deleteListener, itemClickListener);
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView emailView;
        private final Button deleteButton;

        ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_profile_name);
            emailView = itemView.findViewById(R.id.item_profile_email);
            deleteButton = itemView.findViewById(R.id.item_delete_button);
        }

        void bind(UserSummary profile, OnProfileDeleteListener deleteListener, OnItemClickListener itemClickListener) { // ADDED: Added listener param
            nameView.setText(profile.getName());
            emailView.setText(profile.getEmail());
            deleteButton.setOnClickListener(v -> deleteListener.onDelete(profile));
            itemView.setOnClickListener(v -> itemClickListener.onItemClick(profile)); // ADDED: Row click listener
        }
    }

    private static class ProfileDiffCallback extends DiffUtil.ItemCallback<UserSummary> {
        @Override
        public boolean areItemsTheSame(@NonNull UserSummary oldItem, @NonNull UserSummary newItem) {
            return Objects.equals(oldItem.getUserId(), newItem.getUserId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserSummary oldItem, @NonNull UserSummary newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                    Objects.equals(oldItem.getEmail(), newItem.getEmail());
        }
    }
}
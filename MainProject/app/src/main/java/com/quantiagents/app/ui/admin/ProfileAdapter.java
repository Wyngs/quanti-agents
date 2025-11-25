package com.quantiagents.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.quantiagents.app.R;
import com.quantiagents.app.models.User;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    private final List<User> profileList;
    private final OnDeleteClickListener listener;

    public interface OnDeleteClickListener { void onDeleteClick(User user, int position); }

    public ProfileAdapter(List<User> profileList, OnDeleteClickListener listener) {
        this.profileList = profileList;
        this.listener = listener;
    }

    @NonNull @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_admin, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        holder.bind(profileList.get(position), listener);
    }

    @Override
    public int getItemCount() { return profileList.size(); }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        final TextView profileName, profileDetails;
        final ImageButton deleteButton;

        ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            profileName = itemView.findViewById(R.id.text_view_profile_name);
            profileDetails = itemView.findViewById(R.id.text_view_profile_details);
            deleteButton = itemView.findViewById(R.id.button_delete_profile);
        }

        void bind(final User user, final OnDeleteClickListener listener) {
            profileName.setText(user.getName());
            profileDetails.setText("Email: " + user.getEmail());
            deleteButton.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(user, getAdapterPosition());
                }
            });
        }
    }
}
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

/**
 * Adapter for displaying user profiles in admin management screens.
 * Allows admins to view profile details and delete profiles.
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    private final List<User> profileList;
    private final OnDeleteClickListener listener;

    /**
     * Interface for handling profile deletion.
     */
    public interface OnDeleteClickListener {
        /**
         * Called when admin wants to delete a profile.
         *
         * @param user The user profile to delete
         * @param position The position of the profile in the list
         */
        void onDeleteClick(User user, int position);
    }

    /**
     * Constructor that initializes the adapter with profiles and a delete listener.
     *
     * @param profileList The list of profiles to display
     * @param listener The callback interface for handling profile deletion
     */
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

        /**
         * Binds a profile to the view holder, displaying user information and delete button.
         *
         * @param user The user profile to display
         * @param listener The listener for delete actions
         */
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
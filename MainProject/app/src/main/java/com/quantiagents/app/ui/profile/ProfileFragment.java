package com.quantiagents.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.User;

public class ProfileFragment extends Fragment {

    private TextView nameTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private UserService userService;

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        App app = (App) requireActivity().getApplication();
        userService = app.locator().userService();

        nameTextView = view.findViewById(R.id.profile_name);
        emailTextView = view.findViewById(R.id.profile_email);
        phoneTextView = view.findViewById(R.id.profile_phone);
        Button editProfileButton = view.findViewById(R.id.edit_profile_button);

        editProfileButton.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), EditProfileActivity.class));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        userService.getCurrentUser().addOnSuccessListener(user -> {
            if (user != null && getContext() != null) {
                nameTextView.setText(user.getName());
                emailTextView.setText(user.getEmail());
                phoneTextView.setText(user.getPhone());
            }
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
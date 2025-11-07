package com.quantiagents.app.ui.profile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.User;

/**
 * Lets entrants tweak name/email/phone in one place.
 * Uses Task-based async calls so it matches the shared UserService.
 */
public class EditProfileActivity extends AppCompatActivity {

    private UserService userService;

    private TextInputEditText nameField;
    private TextInputEditText emailField;
    private TextInputEditText phoneField;
    private Button saveButton;

    private User currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        App app = (App) getApplication();
        userService = app.locator().userService();

        nameField = findViewById(R.id.edit_profile_name);
        emailField = findViewById(R.id.edit_profile_email);
        phoneField = findViewById(R.id.edit_profile_phone);
        saveButton = findViewById(R.id.save_profile_button);

        loadUserProfile();
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void loadUserProfile() {
        // Fetch the current user asynchronously
        userService.getCurrentUser()
                .addOnSuccessListener(user -> {
                    if (user != null) {
                        currentUser = user;
                        nameField.setText(user.getName());
                        emailField.setText(user.getEmail());
                        phoneField.setText(user.getPhone());
                    } else {
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void saveProfile() {
        if (currentUser == null) {
            Toast.makeText(this, "No user data to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = safe(nameField);
        String email = safe(emailField);
        String phone = safe(phoneField);

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);

        // Update via Task-based API so it matches the rest of the codebase
        userService.updateUser(currentUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                );
    }

    private static String safe(TextInputEditText f) {
        return f.getText() == null ? "" : f.getText().toString().trim();
    }
}

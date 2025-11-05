package com.quantiagents.app.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.models.User;
import com.quantiagents.app.Services.UserService;

public class EditProfileActivity extends AppCompatActivity {

    private UserService userService;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText nameField;
    private TextInputEditText emailField;
    private TextInputEditText phoneField;
    private TextInputEditText passwordField;
    private TextInputEditText confirmPasswordField;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        userService = ((App) getApplication()).locator().userService();
        bindViews();
        bindUser();
        MaterialButton saveButton = findViewById(R.id.button_save_profile);
        saveButton.setOnClickListener(v -> handleSave());
        MaterialButton cancelButton = findViewById(R.id.button_cancel_edit);
        cancelButton.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.edit_name_layout);
        emailLayout = findViewById(R.id.edit_email_layout);
        phoneLayout = findViewById(R.id.edit_phone_layout);
        passwordLayout = findViewById(R.id.edit_password_layout);
        confirmPasswordLayout = findViewById(R.id.edit_confirm_password_layout);
        nameField = findViewById(R.id.edit_name);
        emailField = findViewById(R.id.edit_email);
        phoneField = findViewById(R.id.edit_phone);
        passwordField = findViewById(R.id.edit_password);
        confirmPasswordField = findViewById(R.id.edit_confirm_password);
    }

    private void bindUser() {
        // Use async getCurrentUser to avoid blocking the main thread
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        finish();
                        return;
                    }
                    nameField.setText(user.getName());
                    emailField.setText(user.getEmail());
                    phoneField.setText(user.getPhone());
                    passwordField.setText("");
                    confirmPasswordField.setText("");
                },
                e -> {
                    finish();
                }
        );
    }

    private void handleSave() {
        // Run the same validations before writing back.
        clearErrors();
        String name = safeText(nameField);
        String email = safeText(emailField);
        String phone = safeText(phoneField);
        String newPassword = safeText(passwordField);
        String confirmPassword = safeText(confirmPasswordField);
        boolean hasError = false;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.error_name_required));
            hasError = true;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.error_email_required));
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_email_invalid));
            hasError = true;
        }

        if (!TextUtils.isEmpty(newPassword)) {
            if (newPassword.length() < 6) {
                passwordLayout.setError(getString(R.string.error_password_requirement));
                hasError = true;
            } else if (!newPassword.equals(confirmPassword)) {
                confirmPasswordLayout.setError(getString(R.string.error_password_mismatch));
                hasError = true;
            }
        } else if (!TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_password_requirement));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        try {
            userService.updateUser(name, email, phone);
            if (!TextUtils.isEmpty(newPassword)) {
                userService.updatePassword(newPassword);
            }
            Toast.makeText(this, R.string.message_profile_updated, Toast.LENGTH_SHORT).show();
            finish();
        } catch (IllegalArgumentException exception) {
            emailLayout.setError(getString(R.string.error_email_invalid));
        }
    }

    private void clearErrors() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        phoneLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}

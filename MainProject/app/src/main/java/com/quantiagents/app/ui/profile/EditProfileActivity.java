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
import com.quantiagents.app.Services.UserService;

/**
 * Lets entrants tweak name/email/phone and optionally refresh passwords in one place.
 */
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
    private MaterialButton saveButton;

    @Override
    /**
     * Sets up the form, listeners, and then loads the current profile snapshot.
     */
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        userService = ((App) getApplication()).locator().userService();
        bindViews();
        saveButton = findViewById(R.id.button_save_profile);
        MaterialButton cancelButton = findViewById(R.id.button_cancel_edit);
        saveButton.setOnClickListener(v -> handleSave());
        cancelButton.setOnClickListener(v -> finish());
        bindUser();
    }

    /**
     * Collects all view references in one place to keep onCreate readable.
     */
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

    /**
     * Fetches the active user asynchronously and hydrates the form.
     */
    private void bindUser() {
        saveButton.setEnabled(false);
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        Toast.makeText(this, R.string.error_profile_missing, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    nameField.setText(user.getName());
                    emailField.setText(user.getEmail());
                    phoneField.setText(user.getPhone());
                    passwordField.setText("");
                    confirmPasswordField.setText("");
                    saveButton.setEnabled(true);
                },
                e -> {
                    Toast.makeText(this, R.string.error_profile_missing, Toast.LENGTH_SHORT).show();
                    finish();
                }
        );
    }

    /**
     * Runs form validation, updates Firestore, and handles optional password changes.
     */
    private void handleSave() {
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

        saveButton.setEnabled(false);
        userService.updateUser(
                name,
                email,
                phone,
                aVoid -> {
                    if (!TextUtils.isEmpty(newPassword)) {
                        userService.updatePassword(
                                newPassword,
                                pass -> {
                                    Toast.makeText(this, R.string.message_profile_updated, Toast.LENGTH_SHORT).show();
                                    finish();
                                },
                                e -> {
                                    saveButton.setEnabled(true);
                                    passwordLayout.setError(getString(R.string.error_password_requirement));
                                }
                        );
                    } else {
                        Toast.makeText(this, R.string.message_profile_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                },
                e -> {
                    saveButton.setEnabled(true);
                    emailLayout.setError(getString(R.string.error_email_invalid));
                }
        );
    }

    /**
     * Resets error labels for every TextInputLayout on the screen.
     */
    private void clearErrors() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        phoneLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
    }

    /**
     * Consistent way to read trimmed text without null checks.
     */
    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}

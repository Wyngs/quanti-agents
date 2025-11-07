package com.quantiagents.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.User;
import com.quantiagents.app.ui.admin.AdminDashboardActivity;
import com.quantiagents.app.ui.main.MainActivity;

/**
 * Simple entrant sign-up form to capture name, email, password, and phone on device.
 */
public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout phoneLayout;
    private TextInputEditText nameField;
    private TextInputEditText emailField;
    private TextInputEditText passwordField;
    private TextInputEditText phoneField;
    private MaterialButton createButton;

    private UserService userService;
    private LoginService loginService;

    /**
     * Wires up the form and hooks the create/cancel buttons.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        App app = (App) getApplication();
        userService = app.locator().userService();
        loginService = app.locator().loginService();

        bindViews();

        createButton = findViewById(R.id.button_create_profile);
        MaterialButton cancelButton = findViewById(R.id.button_cancel_sign_up);

        createButton.setOnClickListener(v -> handleCreateProfile());
        cancelButton.setOnClickListener(v -> finish());
    }

    /** Centralizes all findViewById calls for readability. */
    private void bindViews() {
        nameLayout = findViewById(R.id.input_name_layout);
        emailLayout = findViewById(R.id.input_email_layout);
        passwordLayout = findViewById(R.id.input_password_layout);
        phoneLayout = findViewById(R.id.input_phone_layout);

        nameField = findViewById(R.id.input_name);
        emailField = findViewById(R.id.input_email);
        passwordField = findViewById(R.id.input_password);
        phoneField = findViewById(R.id.input_phone);
    }

    /** Validates the form then kicks off the async create → login → route chain. */
    private void handleCreateProfile() {
        clearErrors();

        String name = safeText(nameField);
        String email = safeText(emailField);
        String password = safeText(passwordField);
        String phone = safeText(phoneField);

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
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_password_requirement));
            hasError = true;
        }

        if (hasError) return;

        createButton.setEnabled(false);

        // createNewUser validates and writes to Firestore, returning the new doc id on success
        try {
            userService.createNewUser(
                    name, email, phone, password,
                    userId -> {
                        // Auto-login after creating the profile
                        loginService.login(email, password)
                                .addOnSuccessListener(success -> {
                                    createButton.setEnabled(true);
                                    if (!success) {
                                        emailLayout.setError(getString(R.string.error_login_invalid));
                                        return;
                                    }

                                    Toast.makeText(
                                            this,
                                            getString(R.string.message_profile_created, name),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    // Decide route based on role
                                    userService.getCurrentUser()
                                            .addOnSuccessListener(user -> {
                                                if (user != null && user.getRole() == constant.UserRole.ADMIN) {
                                                    openAdminHome();
                                                } else {
                                                    openHome();
                                                }
                                            })
                                            .addOnFailureListener(e -> openHome());
                                })
                                .addOnFailureListener(e -> {
                                    createButton.setEnabled(true);
                                    emailLayout.setError(getString(R.string.error_login_invalid));
                                });
                    },
                    e -> {
                        createButton.setEnabled(true);
                        // Surface a friendly message; email may already exist or validation failed server-side
                        emailLayout.setError(getString(R.string.error_email_invalid));
                    }
            );
        } catch (IllegalArgumentException ex) {
            createButton.setEnabled(true);
            // Client-side validation exception (should be rare since we check above)
            emailLayout.setError(getString(R.string.error_email_invalid));
        }
    }

    /** Opens MainActivity while clearing the back stack so the user can't return to auth screens. */
    private void openHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Routes admins to the admin dashboard. */
    private void openAdminHome() {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Resets all TextInputLayout errors in one shot. */
    private void clearErrors() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        phoneLayout.setError(null);
    }

    /** Helper to handle null EditText content without sprinkling null checks everywhere. */
    private String safeText(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}

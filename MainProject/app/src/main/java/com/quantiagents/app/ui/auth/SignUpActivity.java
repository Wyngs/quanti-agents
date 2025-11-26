package com.quantiagents.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.ui.main.MainActivity;

/**
 * Simple entrant sign-up form so I can capture name, email, and password on device.
 */
public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout nameLayout;
    private TextInputLayout usernameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout phoneLayout;
    private TextInputEditText nameField;
    private TextInputEditText usernameField;
    private TextInputEditText emailField;
    private TextInputEditText passwordField;
    private TextInputEditText phoneField;
    private MaterialButton createButton;
    private UserService userService;
    private LoginService loginService;

    @Override
    /**
     * Wires up the form and hooks the create/cancel buttons; nothing fancy happens before this.
     */
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        App app = (App) getApplication();
        userService = app.locator().userService();
        loginService = app.locator().loginService();
        bindViews();

        createButton = findViewById(R.id.button_create_profile);
        MaterialButton cancelButton = findViewById(R.id.button_cancel_sign_up);
        MaterialButton loginButton = findViewById(R.id.button_login_instead);

        createButton.setOnClickListener(v -> handleCreateProfile());

        // Both "Cancel" and "Log in instead" now route to the Login screen
        // to prevent the app from closing if the user lands here from SplashActivity.
        cancelButton.setOnClickListener(v -> navigateToLogin());
        loginButton.setOnClickListener(v -> navigateToLogin());
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /**
     * Centralizes all findViewById calls for readability.
     */
    private void bindViews() {
        nameLayout = findViewById(R.id.input_name_layout);
        usernameLayout = findViewById(R.id.input_username_layout);
        emailLayout = findViewById(R.id.input_email_layout);
        passwordLayout = findViewById(R.id.input_password_layout);
        phoneLayout = findViewById(R.id.input_phone_layout);
        nameField = findViewById(R.id.input_name);
        usernameField = findViewById(R.id.input_username);
        emailField = findViewById(R.id.input_email);
        passwordField = findViewById(R.id.input_password);
        phoneField = findViewById(R.id.input_phone);
    }

    /**
     * Validates the form then kicks off the async create/login chain.
     */
    private void handleCreateProfile() {
        clearErrors();
        String name = safeText(nameField);
        String username = safeText(usernameField);
        String email = safeText(emailField);
        String password = safeText(passwordField);
        String phone = safeText(phoneField);
        boolean hasError = false;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.error_name_required));
            hasError = true;
        }

        if (TextUtils.isEmpty(username) || username.length() < 4) {
            usernameLayout.setError(getString((R.string.error_username_requirement)));
            hasError = true;
        } else if (username.trim().contains(" ")) {
            usernameLayout.setError(getString(R.string.error_username_spaces));
            hasError = true;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.error_email_required));
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_email_invalid));
            hasError = true;
        } else {
            String tld = email.substring(email.trim().lastIndexOf('.') + 1);
            if (tld.length() < 2) {
                emailLayout.setError(getString(R.string.error_email_invalid));
                hasError = true;
            }
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_password_requirement));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        createButton.setEnabled(false);

        userService.createUser(
                name,
                username,
                email,
                phone,
                password,
                user -> loginService.login(
                        email,
                        password,
                        success -> {
                            createButton.setEnabled(true);
                            if (success) {
                                Toast.makeText(
                                        this,
                                        getString(R.string.message_profile_created, user.getName()),
                                        Toast.LENGTH_SHORT
                                ).show();
                                openHome();
                            } else {
                                passwordLayout.setError(getString(R.string.error_login_invalid));
                            }
                        },
                        e -> {
                            createButton.setEnabled(true);
                            passwordLayout.setError(getString(R.string.error_login_invalid));
                        }
                ),
                e -> {
                    if (e.getMessage().equals("Username Taken")) {
                        createButton.setEnabled(true);
                        usernameLayout.setError(getString(R.string.error_username_taken));
                    } else if (e.getMessage().equals("Email Taken")) {
                        createButton.setEnabled(true);
                        emailLayout.setError(getString(R.string.error_email_taken));
                    } else {
                        createButton.setEnabled(true);
                        emailLayout.setError(getString(R.string.error_email_invalid));
                    }
                }
        );
    }

    /**
     * Opens MainActivity while clearing the back stack so the user can't return to auth screens.
     */
    private void openHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Resets all TextInputLayout errors in one shot.
     */
    private void clearErrors() {
        nameLayout.setError(null);
        usernameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        phoneLayout.setError(null);
    }

    /**
     * Helper to handle null EditText content without littering null checks everywhere.
     */
    private String safeText(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
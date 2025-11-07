package com.quantiagents.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
 * Handles email/password login plus the fallback reset path when the profile is missing/corrupted.
 */
public class LoginActivity extends AppCompatActivity {

    private UserService userService;
    private LoginService loginService;

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailField;
    private TextInputEditText passwordField;

    /**
     * Loads the saved profile (if any) then wires up login/reset actions.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        App app = (App) getApplication();
        userService = app.locator().userService();
        loginService = app.locator().loginService();

        bindViews();

        // Prefill from current user (by deviceId); if no profile, send to SignUp.
        userService.getCurrentUser()
                .addOnSuccessListener(user -> {
                    if (user == null) {
                        Toast.makeText(this, R.string.error_profile_missing, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, SignUpActivity.class));
                        finish();
                        return;
                    }
                    preloadInputs(user);

                    findViewById(R.id.button_continue).setOnClickListener(v -> handleLogin());
                    findViewById(R.id.button_switch_user).setOnClickListener(v -> resetProfile());

                    TextView forgotView = findViewById(R.id.text_forgot_password);
                    forgotView.setOnClickListener(v ->
                            Toast.makeText(this, R.string.message_password_hint, Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.error_profile_missing, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, SignUpActivity.class));
                    finish();
                });
    }

    /** Centralizes all findViewById calls for readability. */
    private void bindViews() {
        emailLayout = findViewById(R.id.login_username_layout);
        passwordLayout = findViewById(R.id.login_password_layout);
        emailField = findViewById(R.id.input_username);
        passwordField = findViewById(R.id.input_password);
    }

    /** Prefills email and clears password so the user can just enter the secret again. */
    private void preloadInputs(User user) {
        emailField.setText(user.getEmail());
        passwordField.setText("");
    }

    /** Validates inputs, calls async login, routes based on role, surfaces errors inline. */
    private void handleLogin() {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = safeText(emailField);
        String password = safeText(passwordField);

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            emailLayout.setError(getString(R.string.error_login_required));
            passwordLayout.setError(getString(R.string.error_login_required));
            return;
        }

        loginService.login(email, password)
                .addOnSuccessListener(success -> {
                    if (!success) {
                        passwordLayout.setError(getString(R.string.error_login_invalid));
                        return;
                    }

                    // Fetch user to decide where to route (admin vs regular).
                    userService.getCurrentUser()
                            .addOnSuccessListener(user -> {
                                if (user != null && user.getRole() == constant.UserRole.ADMIN) {
                                    openAdminHome();
                                } else {
                                    openHome();
                                }
                            })
                            .addOnFailureListener(e -> {
                                // If we can’t load the user doc, fall back to main app.
                                openHome();
                            });
                })
                .addOnFailureListener(e -> passwordLayout.setError(getString(R.string.error_login_invalid)));
    }

    /** Nukes current profile and sends to SignUp. */
    private void resetProfile() {
        loginService.logout();
        userService.getCurrentUser()
                .addOnSuccessListener(user -> {
                    if (user != null && user.getUserId() != null && !user.getUserId().trim().isEmpty()) {
                        userService.deleteUserProfile(user.getUserId());
                    }
                    Intent intent = new Intent(this, SignUpActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Intent intent = new Intent(this, SignUpActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    /** Starts MainActivity with the usual task-clearing flags. */
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

    /** Utility to fetch trimmed text without sprinkling null guards everywhere. */
    private String safeText(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}

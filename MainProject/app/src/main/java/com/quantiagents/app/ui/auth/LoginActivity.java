package com.quantiagents.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
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
import com.quantiagents.app.models.User;
import com.quantiagents.app.ui.main.MainActivity;

/**
 * Handles email/password login.
 * Dynamically adjusts the secondary action button ("Reset" vs "Create") based on whether a local profile exists.
 */
public class LoginActivity extends AppCompatActivity {

    private UserService userService;
    private LoginService loginService;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailField;
    private TextInputEditText passwordField;
    private MaterialButton switchUserButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        App app = (App) getApplication();
        userService = app.locator().userService();
        loginService = app.locator().loginService();
        bindViews();

        // Default state: Button is set to "Create profile" via XML.
        // Default listener: Navigate to SignUp.
        switchUserButton.setOnClickListener(v -> navigateToSignUp());

        findViewById(R.id.button_continue).setOnClickListener(v -> handleLogin());

        TextView forgotView = findViewById(R.id.text_forgot_password);
        forgotView.setOnClickListener(v ->
                Toast.makeText(this, R.string.message_password_hint, Toast.LENGTH_SHORT).show());

        // Check for local user to determine if we need to switch to "Reset Profile"
        userService.getCurrentUser(
                user -> {
                    if (user != null) {
                        // Case 1: Local profile exists. Switch text and listener to Reset mode.
                        preloadInputs(user);
                        switchUserButton.setText(R.string.login_reset_action);
                        switchUserButton.setOnClickListener(v -> resetProfile());
                    }
                    // Case 2: User is null. Keep default XML text ("Create profile") and listener.
                },
                e -> {
                    // Error case: Keep default "Create profile" state to allow escape.
                }
        );
    }

    private void bindViews() {
        emailLayout = findViewById(R.id.login_username_layout);
        passwordLayout = findViewById(R.id.login_password_layout);
        emailField = findViewById(R.id.input_username);
        passwordField = findViewById(R.id.input_password);
        switchUserButton = findViewById(R.id.button_switch_user);
    }

    private void preloadInputs(User user) {
        emailField.setText(user.getEmail());
        passwordField.setText("");
    }

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
        loginService.login(
                email,
                password,
                success -> {
                    if (success) {
                        // Mark session as active
                        getSharedPreferences("quanti_agents_prefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("user_session_active", true)
                                .apply();
                        openHome();
                    } else {
                        passwordLayout.setError(getString(R.string.error_login_invalid));
                    }
                },
                e -> passwordLayout.setError(getString(R.string.error_login_invalid))
        );
    }

    /**
     * Destructive action: Clears local profile linkage/ID, then goes to Sign Up.
     */
    private void resetProfile() {
        loginService.logout();

        getSharedPreferences("quanti_agents_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("user_session_active", false)
                .apply();

        // Reset local device ID. Next time the app needs an ID, it generates a new one,
        // effectively "orphaning" the previous connection on this phone without wiping the server data.
        ((App) getApplication()).locator().deviceIdManager().reset();

        navigateToSignUp();
    }

    /**
     * Simple navigation action: Goes to Sign Up without deleting anything (since nothing exists).
     */
    private void navigateToSignUp() {
        Intent intent = new Intent(this, SignUpActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String safeText(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
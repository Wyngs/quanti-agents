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
import com.quantiagents.app.models.User;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.ui.main.MainActivity;
import com.quantiagents.app.ui.admin.AdminDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private UserService userService;
    private LoginService loginService;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailField;
    private TextInputEditText passwordField;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        App app = (App) getApplication();
        userService = app.locator().userService();
        loginService = app.locator().loginService();
        bindViews();

        userService.getCurrentUser().addOnSuccessListener(user -> {
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
            forgotView.setOnClickListener(v -> Toast.makeText(this, R.string.message_password_hint, Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e -> {
            Toast.makeText(this, R.string.error_profile_missing, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }

    private void bindViews() {
        emailLayout = findViewById(R.id.login_username_layout);
        passwordLayout = findViewById(R.id.login_password_layout);
        emailField = findViewById(R.id.input_username);
        passwordField = findViewById(R.id.input_password);
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

        loginService.login(email, password).addOnSuccessListener(success -> {
            if (success) {
                userService.getCurrentUser().addOnSuccessListener(user -> {
                    if (user != null && user.getRole() == constant.UserRole.ADMIN) {
                        openAdminHome();
                    } else {
                        openHome();
                    }
                    finish();
                }).addOnFailureListener(e -> {
                    openHome();
                    finish();
                });
            } else {
                passwordLayout.setError(getString(R.string.error_login_invalid));
            }
        }).addOnFailureListener(e -> {
            passwordLayout.setError(getString(R.string.error_login_invalid));
        });
    }

    private void openHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openAdminHome() {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resetProfile() {
        loginService.logout();
        userService.getCurrentUser().addOnSuccessListener(user-> {
            if (user != null) {
                userService.deleteUserProfile(user.getUserId());
            }
            Intent intent = new Intent(this, SignUpActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String safeText(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
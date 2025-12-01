package com.quantiagents.app.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.ui.main.MainActivity;

/**
 * Bootstraps the app by checking device login first, then routing to login or home.
 */
public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    /**
     * Shows the splash layout briefly, then hands control to {@link #routeToNextScreen()}.
     */
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.post(this::routeToNextScreen);
    }

    /**
     * Determines the correct destination based on device login and stored profile state.
     */
    private void routeToNextScreen() {
        App app = (App) getApplication();
        DeviceIdManager deviceIdManager = app.locator().deviceIdManager();
        LoginService loginService = app.locator().loginService();
        
        SharedPreferences prefs = getSharedPreferences("quanti_agents_prefs", MODE_PRIVATE);
        boolean sessionActive = prefs.getBoolean("user_session_active", true);
        boolean rememberMe = prefs.getBoolean("remember_me", true);

        // Check if the user explicitly logged out previously or "Remember Me" was not checked.
        if (!sessionActive || !rememberMe) {
            // User logged out or didn't opt for auto-login. Go to Login.
            launchLogin();
            finish();
            return;
        }

        // "Remember Me" was checked, attempt auto-login
        String deviceId = deviceIdManager.ensureDeviceId();
        loginService.loginWithDevice(
                deviceId,
                success -> {
                    if (success) {
                        launchHome();
                    } else {
                        // Auto-login failed (no user or error) -> Login Screen
                        launchLogin();
                    }
                    finish();
                },
                e -> {
                    // Error -> Fallback to Login Screen
                    launchLogin();
                    finish();
                }
        );
    }

    /**
     * Opens LoginActivity. This is the default entry point if auto-login fails.
     */
    private void launchLogin() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    /**
     * Launches MainActivity directly when device login succeeds.
     */
    private void launchHome() {
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
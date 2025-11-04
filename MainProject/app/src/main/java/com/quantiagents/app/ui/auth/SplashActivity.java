package com.quantiagents.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.post(this::routeToNextScreen);
    }

    private void routeToNextScreen() {
        App app = (App) getApplication();
        DeviceIdManager deviceIdManager = app.locator().deviceIdManager();
        UserService userService = app.locator().userService();
        LoginService loginService = app.locator().loginService();
        String deviceId = deviceIdManager.ensureDeviceId();
        // Try silent device login first; fall back to manual entry.
        loginService.loginWithDevice(deviceId,
                success -> {
                    if (success) {
                        launchHome();
                        finish();
                    } else {
                        // Check if user exists to decide between sign up and login
                        userService.getCurrentUser(
                                user -> {
                                    if (user == null) {
                                        launchSignUp();
                                    } else {
                                        launchLogin();
                                    }
                                    finish();
                                },
                                e -> {
                                    // On error, go to sign up
                                    launchSignUp();
                                    finish();
                                }
                        );
                    }
                },
                e -> {
                    // On error, check if user exists
                    userService.getCurrentUser(
                            user -> {
                                if (user == null) {
                                    launchSignUp();
                                } else {
                                    launchLogin();
                                }
                                finish();
                            },
                            err -> {
                                // On error, go to sign up
                                launchSignUp();
                                finish();
                            }
                    );
                }
        );
    }

    private void launchSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    private void launchLogin() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    private void launchHome() {
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

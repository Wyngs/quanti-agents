package com.quantiagents.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
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
        UserService userService = app.locator().userService();
        LoginService loginService = app.locator().loginService();
        String deviceId = deviceIdManager.ensureDeviceId();

        loginService.loginWithDevice(
                deviceId,
                success -> {
                    if (success) {
                        launchHome();
                        finish();
                        return;
                    }
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
                                launchLogin();
                                finish();
                            }
                    );
                },
                e -> userService.getCurrentUser(
                        user -> {
                            if (user == null) {
                                launchSignUp();
                            } else {
                                launchLogin();
                            }
                            finish();
                        },
                        ignored -> {
                            launchLogin();
                            finish();
                        }
                )
        );
    }

    /**
     * Sends the user to SignUpActivity without clearing task (splash already finishes).
     */
    private void launchSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    /**
     * Opens LoginActivity when we have a profile but still need credentials.
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

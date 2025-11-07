package com.quantiagents.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.ui.admin.AdminDashboardActivity;
import com.quantiagents.app.ui.main.MainActivity;

/**
 * Bootstraps the app by checking device login first, then routing to login or home.
 */
public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String TAG = "SplashActivity";

    /** Shows the splash briefly, then routes. */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(this::routeToNextScreen, 1000);
    }

    /** Determines the correct destination based on device login and stored profile state. */
    private void routeToNextScreen() {
        App app = (App) getApplication();
        DeviceIdManager deviceIdManager = app.locator().deviceIdManager();
        UserService userService = app.locator().userService();
        LoginService loginService = app.locator().loginService();

        String deviceId = deviceIdManager.ensureDeviceId();

        // 1) Try device-based login first
        loginService.loginWithDevice(deviceId)
                .addOnSuccessListener(success -> {
                    if (success) {
                        // Device recognized → fetch user to decide admin vs home
                        userService.getCurrentUser()
                                .addOnSuccessListener(user -> {
                                    if (user != null && user.getRole() == constant.UserRole.ADMIN) {
                                        launchAdminHome();
                                    } else {
                                        launchHome();
                                    }
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Device login ok but failed to get user", e);
                                    launchLogin();
                                    finish();
                                });
                        return;
                    }

                    // No device login → see if a profile exists to choose SignUp vs Login
                    userService.getCurrentUser()
                            .addOnSuccessListener(user -> {
                                if (user == null) {
                                    launchSignUp();
                                } else {
                                    launchLogin();
                                }
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to get user while no device login", e);
                                launchSignUp();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Device login task failed", e);
                    launchLogin();
                    finish();
                });
    }

    /** Sends the user to SignUpActivity (splash finishes, no need to clear task). */
    private void launchSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    /** Routes admins to the admin dashboard. */
    private void launchAdminHome() {
        startActivity(new Intent(this, AdminDashboardActivity.class));
    }

    /** Opens LoginActivity. */
    private void launchLogin() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    /** Launches MainActivity directly when device login succeeds. */
    private void launchHome() {
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

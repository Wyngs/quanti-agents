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
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.ui.admin.AdminDashboardActivity;
import com.quantiagents.app.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(this::routeToNextScreen, 1000);
    }

    private void routeToNextScreen() {
        App app = (App) getApplication();
        DeviceIdManager deviceIdManager = app.locator().deviceIdManager();
        UserService userService = app.locator().userService();
        LoginService loginService = app.locator().loginService();
        String deviceId = deviceIdManager.ensureDeviceId();

        loginService.loginWithDevice(deviceId).addOnSuccessListener(success -> {
            if (success) {
                userService.getCurrentUser().addOnSuccessListener(user -> {
                    if (user != null && user.getRole() == constant.UserRole.ADMIN) {
                        launchAdminHome();
                    } else {
                        launchHome();
                    }
                    finish();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Device login success but failed to get user", e);
                    launchLogin();
                    finish();
                });
            } else {
                userService.getCurrentUser().addOnSuccessListener(user -> {
                    if (user == null) {
                        launchSignUp();
                    } else {
                        launchLogin();
                    }
                    finish();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "No device login and failed to get user", e);
                    launchSignUp();
                    finish();
                });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Device login task failed", e);
            launchLogin();
            finish();
        });
    }

    private void launchSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    private void launchAdminHome() {
        startActivity(new Intent(this, AdminDashboardActivity.class));
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
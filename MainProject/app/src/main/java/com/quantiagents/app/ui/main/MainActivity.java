package com.quantiagents.app.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.DeviceIdManager;
import com.quantiagents.app.models.User;
import com.quantiagents.app.ui.CreateEventFragment;
import com.quantiagents.app.ui.NotificationCenterFragment;
import com.quantiagents.app.ui.ScanQRCode.ScanQRCodeFragment;
import com.quantiagents.app.ui.admin.AdminBrowseEventsFragment;
import com.quantiagents.app.ui.admin.AdminBrowseImagesFragment;
import com.quantiagents.app.ui.admin.AdminBrowseNotificationsFragment;
import com.quantiagents.app.ui.admin.AdminBrowseProfilesFragment;
import com.quantiagents.app.ui.auth.LoginActivity;
import com.quantiagents.app.ui.manageevents.ManageEventsFragment;
import com.quantiagents.app.ui.myevents.BrowseEventsFragment;
import com.quantiagents.app.ui.myevents.MyEventFragment;
import com.quantiagents.app.ui.messages.MessagesFragment;
import com.quantiagents.app.ui.profile.ProfileFragment;
import com.quantiagents.app.ui.profile.SettingsFragment;
import com.quantiagents.app.Services.BadgeService;

/**
 * Main activity that serves as the primary navigation hub for the application.
 * Manages the navigation drawer, fragment switching, and user authentication state.
 * Supports both admin and entrant user roles with different menu options.
 */
@OptIn(markerClass = {ExperimentalGetImage.class, ExperimentalBadgeUtils.class})
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private UserService userService;
    private LoginService loginService;
    // changed the landing page from profile details to the browse events page
    private int activeItemId = R.id.navigation_browse_events;

    // Store original menu item titles
    private String originalNotificationTitle;
    private String originalMessageTitle;

    /**
     * Initializes the activity, sets up navigation drawer, and loads user data.
     * Checks if user is logged in and redirects to LoginActivity if not.
     *
     * @param savedInstanceState The saved instance state bundle
     */
    // Permission launcher for Android 13+ Notification permission
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted - immediately try to update the badge
                    updateAppIconBadge();
                } else {
                    // Permission denied - badges on Samsung/Android 8+ won't work reliably
                    Toast.makeText(this, "Notifications required for app icon badges", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        App app = (App) getApplication();
        userService = app.locator().userService();
        loginService = app.locator().loginService();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        // Custom Toolbar Setup
        ImageButton menuButton = findViewById(R.id.toolbar_menu_button);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        navigationView.setNavigationItemSelectedListener(this);

        // 1.Checking if LoginService already has the user in memory (This happens right after LoginActivity)
        User cachedUser = loginService.getActiveUser();

        if (cachedUser != null) {
            // If we have a user in memory, use it immediately
            onUserLoaded(cachedUser, savedInstanceState);
        } else {
            // 2.If memory is empty (e.g., App Restart), fetch from DB using Device ID
            userService.getCurrentUser(
                    user -> {
                        if (user == null) {
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                            return;
                        }
                        onUserLoaded(user, savedInstanceState);
                    },
                    e -> {
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
            );
        }
    }

    /**
     * Helper method to setup UI once user is found.
     * Binds user data to header, sets up admin menu, updates badges, and shows initial fragment.
     *
     * @param user The loaded user object
     * @param savedInstanceState The saved instance state bundle
     */
    private void onUserLoaded(@NonNull User user, Bundle savedInstanceState) {
        bindHeader(user);
        setupAdminMenu(user);

        // Check for notification permission (Required for Badges on Android 13+)
        checkAndRequestNotificationPermission();

        // Update navigation menu badges
        updateNavigationMenuBadges();

        if (savedInstanceState == null) {
            // CHECK FOR NOTIFICATION NAVIGATION EXTRA
            if (getIntent().getBooleanExtra("navigate_to_notifications", false)) {
                activeItemId = R.id.navigation_notifications;
                navigationView.setCheckedItem(activeItemId);
                showFragment(NotificationCenterFragment.newInstance());
            } else {
                navigationView.setCheckedItem(activeItemId);
                showFragment(BrowseEventsFragment.newInstance());
            }
        }
    }

    /**
     * Checks for POST_NOTIFICATIONS permission on Android 13+ and requests it if missing.
     * This is critical for Samsung badges to work.
     */
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Already granted, safe to update badge
                updateAppIconBadge();
            }
        } else {
            // Android 12 or lower doesn't require runtime permission for notifications
            updateAppIconBadge();
        }
    }

    /**
     * Updates the app icon badge with the current unread notification count.
     */
    private void updateAppIconBadge() {
        BadgeService badgeService = new BadgeService(this);
        badgeService.updateBadgeCount();
    }

    /**
     * Binds user information to the navigation drawer header.
     * Displays user name and role (Admin or Entrant).
     *
     * @param user The user object to display
     */
    private void bindHeader(@NonNull User user) {
        View header = navigationView.getHeaderView(0);
        TextView nameView = header.findViewById(R.id.text_logged_in_name);
        TextView roleView = header.findViewById(R.id.text_logged_in_role);
        ImageView closeButton = header.findViewById(R.id.button_close_drawer);

        closeButton.setOnClickListener(v -> drawerLayout.closeDrawers());

        // Keep header state in sync with the active profile.
        nameView.setText(user.getName());

        if (user.getRole() == constant.UserRole.ADMIN) {
            roleView.setText(R.string.nav_role_admin);
        } else {
            roleView.setText(R.string.nav_role_entrant);
        }
    }

    /**
     * Sets up the navigation menu based on user role.
     * Shows admin menu items for admin users, entrant menu items for regular users.
     *
     * @param user The user object to determine role
     */
    private void setupAdminMenu(@NonNull User user) {
        Menu menu = navigationView.getMenu();

        // Checking if user is admin
        boolean isAdmin = (user.getRole() == constant.UserRole.ADMIN);

        if (isAdmin) {
            menu.setGroupVisible(R.id.admin_group, true);
            // hide entrant features (Browse, My Events, Create, etc.)
            menu.setGroupVisible(R.id.group_entrant_features, false);
        } else {
            // hide admin features
            menu.setGroupVisible(R.id.admin_group, false);
            // show entrant features
            menu.setGroupVisible(R.id.group_entrant_features, true);
        }
    }

    /**
     * Handles navigation item selection from the drawer menu.
     * Switches to the appropriate fragment based on the selected menu item.
     *
     * @param item The selected menu item
     * @return True if the item selection was handled
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.navigation_profile) {
            showFragment(ProfileFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_settings) {
            showFragment(SettingsFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_create_event) {
            showFragment(CreateEventFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_browse_events) {
            showFragment(BrowseEventsFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_my_events) {
            showFragment(MyEventFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_admin_events) {
            showFragment(AdminBrowseEventsFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_admin_profiles) {
            showFragment(AdminBrowseProfilesFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_admin_images) {
            showFragment(AdminBrowseImagesFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        }
        else if (id == R.id.navigation_admin_notifications) {
            showFragment(AdminBrowseNotificationsFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        }
        // ---------------------------------
        else if (id == R.id.navigation_notifications) {
            showFragment(NotificationCenterFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_messages) {
            showFragment(MessagesFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_manage_events) {
            showFragment(ManageEventsFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_scan_qr) {
            showFragment(ScanQRCodeFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_logout) {
            handleLogout();
        } else {
            Toast.makeText(this, R.string.nav_placeholder_unavailable, Toast.LENGTH_SHORT).show();
            navigationView.setCheckedItem(activeItemId);
        }
        drawerLayout.closeDrawer(GravityCompat.END);
        return true;
    }

    private void handleLogout() {
        loginService.logout();
        
        // Clear device ID so that when a new user logs in and clicks "Remember Me",
        // their device ID will be saved instead of the previous user's device ID
        DeviceIdManager deviceIdManager = ((App) getApplication()).locator().deviceIdManager();
        deviceIdManager.reset();
        
        // Mark session as inactive and clear "Remember Me" flag so SplashActivity routes to login
        getSharedPreferences("quanti_agents_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("user_session_active", false)
                .putBoolean("remember_me", false)
                .apply();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_container, fragment)
                .commit();
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check permission again on resume in case user toggled it in settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                updateAppIconBadge();
            }
        } else {
            updateAppIconBadge();
        }

        updateNavigationMenuBadges();
    }

    /**
     * Updates the navigation menu badges for Notifications and Messages with unread counts.
     * Can be called from fragments to update badges when notifications/messages change.
     */
    public void updateNavigationMenuBadges() {
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        clearNavigationBadges();
                        return;
                    }

                    BadgeService badgeService = new BadgeService(this);

                    // Get unread notification count
                    badgeService.getUnreadNotificationCount(user.getUserId(),
                            notificationCount -> {
                                updateNotificationBadge(notificationCount);
                            });

                    // Get unread message count
                    badgeService.getUnreadMessageCount(user.getUserId(),
                            messageCount -> {
                                updateMessageBadge(messageCount);
                            });
                },
                e -> {
                    Log.e("MainActivity", "Failed to get current user for navigation badges", e);
                    clearNavigationBadges();
                }
        );
    }

    /**
     * Updates the notification menu item badge with the unread count.
     * Must be called on the main thread.
     */
    private void updateNotificationBadge(int count) {
        // Ensure we're on the main thread
        runOnUiThread(() -> {
            MenuItem notificationItem = navigationView.getMenu().findItem(R.id.navigation_notifications);
            if (notificationItem == null) {
                return;
            }

            // Store original title if not already stored
            if (originalNotificationTitle == null) {
                originalNotificationTitle = notificationItem.getTitle().toString();
            }

            // Update title with count
            if (count > 0) {
                notificationItem.setTitle(originalNotificationTitle + " (" + count + ")");
            } else {
                notificationItem.setTitle(originalNotificationTitle);
            }
        });
    }

    /**
     * Updates the messages menu item badge with the unread count.
     * Must be called on the main thread.
     */
    private void updateMessageBadge(int count) {
        // Ensure we're on the main thread
        runOnUiThread(() -> {
            MenuItem messageItem = navigationView.getMenu().findItem(R.id.navigation_messages);
            if (messageItem == null) {
                return;
            }

            // Store original title if not already stored
            if (originalMessageTitle == null) {
                originalMessageTitle = messageItem.getTitle().toString();
            }

            // Update title with count
            if (count > 0) {
                messageItem.setTitle(originalMessageTitle + " (" + count + ")");
            } else {
                messageItem.setTitle(originalMessageTitle);
            }
        });
    }

    /**
     * Clears all navigation menu badges.
     */
    private void clearNavigationBadges() {
        updateNotificationBadge(0);
        updateMessageBadge(0);
    }
}
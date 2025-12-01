package com.quantiagents.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
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

@OptIn(markerClass = {ExperimentalGetImage.class, ExperimentalBadgeUtils.class})
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private UserService userService;
    private LoginService loginService;
    private int activeItemId = R.id.navigation_profile;
    
    // Store original menu item titles
    private String originalNotificationTitle;
    private String originalMessageTitle;

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
            // prevents fetching the old admin user from the database based on Device ID
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

    // helper method to setup UI once user is found
    private void onUserLoaded(@NonNull User user, Bundle savedInstanceState) {
        bindHeader(user);
        setupAdminMenu(user);

        // Update app icon badge with unread notification count
        updateAppIconBadge();
        
        // Update navigation menu badges
        updateNavigationMenuBadges();

        if (savedInstanceState == null) {
            navigationView.setCheckedItem(activeItemId);
            showFragment(ProfileFragment.newInstance());
        }
    }

    /**
     * Updates the app icon badge with the current unread notification count.
     */
    private void updateAppIconBadge() {
        BadgeService badgeService = new BadgeService(this);
        badgeService.updateBadgeCount();
    }

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
        // Update badge when app resumes (user might have received notifications while app was in background)
        updateAppIconBadge();
        // Update navigation menu badges
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
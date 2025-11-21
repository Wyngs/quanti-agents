package com.quantiagents.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.quantiagents.app.App;
import com.quantiagents.app.Constants.constant;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.LoginService;
import com.quantiagents.app.Services.UserService;
import com.quantiagents.app.models.User;
import com.quantiagents.app.ui.CreateEventFragment;
import com.quantiagents.app.ui.admin.AdminBrowseImagesFragment;
import com.quantiagents.app.ui.admin.AdminBrowseEventsFragment;
import com.quantiagents.app.ui.admin.AdminBrowseProfilesFragment;
import com.quantiagents.app.ui.auth.LoginActivity;
import com.quantiagents.app.ui.manageevents.ManageEventsFragment;
import com.quantiagents.app.ui.myevents.BrowseEventsFragment;
import com.quantiagents.app.ui.myevents.MyEventFragment;
import com.quantiagents.app.ui.profile.ProfileFragment;
import com.quantiagents.app.ui.profile.SettingsFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private UserService userService;
    private LoginService loginService;
    private int activeItemId = R.id.navigation_profile;

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

        // Use async getCurrentUser to avoid blocking the main thread
        userService.getCurrentUser(
                user -> {
                    if (user == null) {
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return;
                    }

                    bindHeader(user);
                    setupAdminMenu(user);

                    if (savedInstanceState == null) {
                        navigationView.setCheckedItem(activeItemId);
                        showFragment(ProfileFragment.newInstance());
                    }
                },
                e -> {
                    // On error, redirect to login
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                }
        );
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
        if (user.getRole() == constant.UserRole.ADMIN) {
            menu.setGroupVisible(R.id.admin_group, true);
        } else {
            menu.setGroupVisible(R.id.admin_group, false);
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
        } else if (id == R.id.navigation_admin_images) {
            showFragment(AdminBrowseImagesFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_admin_profiles) {
            showFragment(AdminBrowseProfilesFragment.newInstance());
            activeItemId = id;
            navigationView.setCheckedItem(id);
        } else if (id == R.id.navigation_manage_events) {
            showFragment(ManageEventsFragment.newInstance());
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
}
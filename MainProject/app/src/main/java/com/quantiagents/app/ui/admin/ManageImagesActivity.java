package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.AdminService;
import com.quantiagents.app.models.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageImagesActivity extends AppCompatActivity {

    private AdminService adminService;
    private ImageAdapter adapter;
    private final List<Image> imageList = new ArrayList<>();
    private ProgressBar progressBar;
    private View rootView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_images);

        App app = (App) getApplication();
        adminService = app.locator().adminService();

        rootView = findViewById(android.R.id.content);
        progressBar = findViewById(R.id.progress_bar_images);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_images);
        Toolbar toolbar = findViewById(R.id.toolbar_manage_images);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Images");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new ImageAdapter(imageList, this::deleteImage);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadImages();
    }

    private void loadImages() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<Image> images = adminService.listAllImages();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                imageList.clear();
                if (images != null) imageList.addAll(images);
                adapter.notifyDataSetChanged();
                if (imageList.isEmpty()) {
                    Toast.makeText(this, "No images found.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void deleteImage(Image image, int position) {
        // Call AdminService with callbacks
        adminService.removeImage(
                image.getImageId(),
                true,
                "Admin deleted",
                aVoid -> {
                    // On Success
                    imageList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, imageList.size());
                    Snackbar.make(rootView, "Image deleted.", Snackbar.LENGTH_LONG).show();
                },
                e -> {
                    // On Failure
                    Snackbar.make(rootView, "Failed to delete image.", Snackbar.LENGTH_LONG).show();
                }
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
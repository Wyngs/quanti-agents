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

public class ManageImagesActivity extends AppCompatActivity {

    private AdminService adminService;
    private ImageAdapter adapter;
    private final List<Image> imageList = new ArrayList<>();
    private ProgressBar progressBar;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_images); // Changed from activity_manage_posters

        App app = (App) getApplication();
        adminService = app.locator().adminService();

        rootView = findViewById(android.R.id.content);
        progressBar = findViewById(R.id.progress_bar_images); // Changed from progress_bar_posters
        RecyclerView recyclerView = findViewById(R.id.recycler_view_images); // Changed from recycler_view_posters
        Toolbar toolbar = findViewById(R.id.toolbar_manage_images); // Changed from toolbar_manage_posters

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Images");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupRecyclerView(recyclerView);
        loadImages();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        adapter = new ImageAdapter(imageList, this::deleteImage);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadImages() {
        progressBar.setVisibility(View.VISIBLE);

        adminService.listAllImages().addOnSuccessListener(querySnapshot -> {
            progressBar.setVisibility(View.GONE);
            List<Image> loadedImages = querySnapshot.toObjects(Image.class);
            if (loadedImages != null && !loadedImages.isEmpty()) {
                imageList.clear();
                imageList.addAll(loadedImages);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "No images found.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error loading images.", Toast.LENGTH_LONG).show();
        });
    }

    private void deleteImage(Image image, int position) {
        adminService.removeImage(image.getImageId(), true, "Admin deleted image")
                .addOnSuccessListener(aVoid -> {
                    imageList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, imageList.size());
                    Snackbar.make(rootView, "Image deleted successfully.", Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(rootView, "Failed to delete image.", Snackbar.LENGTH_LONG).show();
                });
    }
}
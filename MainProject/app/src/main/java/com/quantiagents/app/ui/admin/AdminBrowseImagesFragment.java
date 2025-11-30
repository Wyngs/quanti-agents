package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.text.Editable; // ADDED: Import for search
import android.text.TextWatcher; // ADDED: Import for search listener
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText; // ADDED: Import for EditText
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.ui.admin.viewmodel.AdminEventsViewModel;

public class AdminBrowseImagesFragment extends Fragment {

    private AdminEventsViewModel viewModel;
    private AdminImageAdapter adapter;

    public static AdminBrowseImagesFragment newInstance() {
        return new AdminBrowseImagesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        viewModel = new ViewModelProvider(this).get(AdminEventsViewModel.class);


        TextView titleView = view.findViewById(R.id.text_admin_title);
        if (titleView != null) {
            titleView.setText("Manage Images");
        }


        RecyclerView recyclerView = view.findViewById(R.id.admin_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        adapter = new AdminImageAdapter(image -> {

            viewModel.deleteImage(image);
        });
        recyclerView.setAdapter(adapter);

        EditText searchInput = view.findViewById(R.id.input_search); // Changed to EditText and updated ID to match XML
        if (searchInput != null) {
            searchInput.setHint("Search Images..."); // Set dynamic hint for images
            searchInput.addTextChangedListener(new TextWatcher() { // Changed listener to TextWatcher for EditText
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.searchImages(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }


        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            if (images != null) {
                adapter.submitList(images);
                if (images.isEmpty()) {

                    Toast.makeText(getContext(), "No images found.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadImages();
    }
}
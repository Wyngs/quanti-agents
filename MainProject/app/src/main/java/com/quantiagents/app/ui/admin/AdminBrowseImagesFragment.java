package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
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

        SearchView searchView = view.findViewById(R.id.admin_search_view);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    viewModel.searchImages(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    viewModel.searchImages(newText);
                    return true;
                }
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
package com.quantiagents.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private RecyclerView recyclerView;
    private AdminImageAdapter adapter;

    public static AdminBrowseImagesFragment newInstance() {
        return new AdminBrowseImagesFragment();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //reusing the generic admin browse layout
        return inflater.inflate(R.layout.fragment_admin_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminEventsViewModel.class);

        recyclerView = view.findViewById(R.id.admin_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminImageAdapter(image -> {

            viewModel.deleteImage(image);
        });
        recyclerView.setAdapter(adapter);

        viewModel.getImages().observe(getViewLifecycleOwner(), images -> {
            adapter.submitList(images);
        });

        //observe toast messages for errors/success
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });

        //trigger the load
        viewModel.loadImages();
    }
}
package com.quantiagents.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.quantiagents.app.R;
import com.quantiagents.app.models.Image;

import java.util.ArrayList;
import java.util.List;

public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.ImageViewHolder> {

    private List<Image> images = new ArrayList<>();
    private final OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Image image);
    }

    public AdminImageAdapter(OnDeleteClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Image> newImages) {
        this.images = newImages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Image image = images.get(position);
        holder.bind(image, listener);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        TextView idView;
        TextView uriView;
        ImageView imageView;
        Button deleteButton;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            idView = itemView.findViewById(R.id.text_admin_image_id);
            uriView = itemView.findViewById(R.id.text_admin_image_uri);
            imageView = itemView.findViewById(R.id.image_admin_preview);
            deleteButton = itemView.findViewById(R.id.button_admin_delete_image);
        }

        public void bind(Image image, OnDeleteClickListener listener) {
            idView.setText(itemView.getContext().getString(R.string.admin_image_id_fmt, image.getImageId()));

            uriView.setText(image.getUri());

            if (image.getUri() != null && !image.getUri().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(image.getUri())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_delete)
                        .into(imageView);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            deleteButton.setOnClickListener(v -> listener.onDeleteClick(image));
        }
    }
}
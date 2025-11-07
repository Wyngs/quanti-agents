package com.quantiagents.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.quantiagents.app.R;
import com.quantiagents.app.models.Image;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final List<Image> imageList;
    private final OnDeleteClickListener listener;

    public interface OnDeleteClickListener { void onDeleteClick(Image image, int position); }

    public ImageAdapter(List<Image> imageList, OnDeleteClickListener listener) {
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_admin, parent, false); // Changed from item_poster_admin
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.bind(imageList.get(position), listener);
    }

    @Override
    public int getItemCount() { return imageList.size(); }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ImageView posterImage;
        final TextView posterDetails;
        final ImageButton deleteButton;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.image_view_item);
            posterDetails = itemView.findViewById(R.id.text_view_image_details);
            deleteButton = itemView.findViewById(R.id.button_delete_image);
        }

        public void bind(final Image image, final OnDeleteClickListener listener) {
            String details = "Image ID: " + image.getImageId() + "\nEvent ID: " + image.getEventId();
            posterDetails.setText(details);

            Glide.with(itemView.getContext())
                    .load(image.getUri())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(posterImage);

            deleteButton.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(image, getAdapterPosition());
                }
            });
        }
    }
}
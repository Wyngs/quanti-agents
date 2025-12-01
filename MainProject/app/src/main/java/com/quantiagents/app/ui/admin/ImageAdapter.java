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

/**
 * Adapter for displaying images in admin management screens.
 * Allows admins to view images and delete them.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final List<Image> imageList;
    private final OnDeleteClickListener listener;

    /**
     * Interface for handling image deletion.
     */
    public interface OnDeleteClickListener {
        /**
         * Called when admin wants to delete an image.
         *
         * @param image The image to delete
         * @param position The position of the image in the list
         */
        void onDeleteClick(Image image, int position);
    }

    /**
     * Constructor that initializes the adapter with images and a delete listener.
     *
     * @param imageList The list of images to display
     * @param listener The callback interface for handling image deletion
     */
    public ImageAdapter(List<Image> imageList, OnDeleteClickListener listener) {
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_admin, parent, false);
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

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.image_view_item);
            posterDetails = itemView.findViewById(R.id.text_view_image_details);
            deleteButton = itemView.findViewById(R.id.button_delete_image);
        }

        /**
         * Binds an image to the view holder, displaying the image and delete button.
         *
         * @param image The image to display
         * @param listener The listener for delete actions
         */
        void bind(final Image image, final OnDeleteClickListener listener) {
            String details = "Image ID: " + image.getImageId();
            posterDetails.setText(details);

            // Using Glide as in your old code. Ensure dependency is in build.gradle.
            // If not, this line will compile if Glide library is present, or break if not.
            // Assuming you have it since it was in the old files.
            try {
                Glide.with(itemView.getContext())
                        .load(image.getUri())
                        .placeholder(R.drawable.ic_launcher_foreground) // Fallback placeholder
                        .into(posterImage);
            } catch (NoClassDefFoundError e) {
                // Fallback if Glide is missing (safety)
                posterImage.setImageResource(R.drawable.ic_launcher_foreground);
            }

            deleteButton.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(image, getAdapterPosition());
                }
            });
        }
    }
}
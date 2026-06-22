package com.example.smartfarmapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * GalleryAdapter
 * ──────────────
 * This class is a "List Manager" for a 2-column grid of photos and videos.
 *
 * What is an Adapter?
 * Think of it like a translator. On one side, you have raw data (a list of URLs from a database).
 * On the other side, you have the phone screen. The Adapter translates that data into 
 * beautiful pictures and text that the user can see and tap on.
 *
 * - Photos: Load automatically from the web using a library called 'Glide'.
 * - Videos: Shown with a play-button icon so the user knows they can click it.
 *
 * Requires Glide in build.gradle:
 *   implementation 'com.github.bumptech.glide:glide:4.16.0'
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    /**
     * --- INTERFACES (Click Listeners) ---
     * These are like "Message Envelopes". When someone clicks an image, the Adapter 
     * sends a message to the Activity/Fragment saying "Hey, the user clicked this item!".
     * The Activity then decides what to do (like opening the image in full screen).
     */
    public interface OnImageClickListener { void onImageClick(FarmGallery item); }
    public interface OnVideoClickListener { void onVideoClick(FarmGallery item); }

    // --- Variables ---
    private final List<FarmGallery>    items;              // Our list of data (photos/videos)
    private final OnImageClickListener imageClickListener; // The "messenger" for image clicks
    private final OnVideoClickListener videoClickListener; // The "messenger" for video clicks

    /**
     * --- CONSTRUCTOR ---
     * This is where we set up the adapter. We give it the list of data and the click handlers.
     * 
     * @param items The list of FarmGallery objects to show.
     * @param imageClickListener What to do when an image is tapped.
     * @param videoClickListener What to do when a video is tapped.
     *
     * Precondition: items is a valid List of FarmGallery objects, click listeners can be null
     * Postcondition: A new GalleryAdapter is created with the provided list and listeners
     */
    public GalleryAdapter(List<FarmGallery> items,
                          OnImageClickListener imageClickListener,
                          OnVideoClickListener videoClickListener) {
        this.items              = items;
        this.imageClickListener = imageClickListener;
        this.videoClickListener = videoClickListener;
    }

    /**
     * --- 1. ON-CREATE-VIEW-HOLDER ---
     * This method creates the "Physical Container" for a single item in our grid.
     * It's only called a few times to create enough "boxes" to fill the screen.
     * 
     * @param parent The grid that will hold the items.
     * @return A new GalleryViewHolder holding our 'item_gallery.xml' layout.
     *
     * Precondition: parent is not null
     * Postcondition: A new GalleryViewHolder is created with the inflated item_gallery layout
     */
    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // We take the XML layout file 'item_gallery' and turn it into a real Java View object.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery, parent, false);
        return new GalleryViewHolder(view);
    }

    /**
     * --- 2. ON-BIND-VIEW-HOLDER ---
     * This is the "Data Filler". It takes one of the boxes (the holder) and fills it 
     * with the info for a specific item (the position).
     * 
     * @param holder The "box" we are filling.
     * @param position Which item in the list we are currently looking at.
     *
     * Precondition: holder is not null and position is within bounds of items list
     * Postcondition: The UI elements in the holder are updated with data from the FarmGallery item at the given position, and click listeners are attached
     */
    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        // Get the specific data item for this position.
        FarmGallery item = items.get(position);
        
        // Display the date when the photo/video was taken.
        holder.tvDate.setText(formatDate(item.getDate()));

        // --- Check if this item is a VIDEO or a PHOTO ---
        if (item.isVideo()) {
            // If it's a VIDEO:
            // 1. Show the "Play" button icon on top of the thumbnail.
            holder.ivPlayOverlay.setVisibility(View.VISIBLE);
            
            // 2. Load a default play icon as the thumbnail.
            Glide.with(holder.itemView.getContext())
                    .load(android.R.drawable.ic_media_play)
                    .into(holder.ivThumbnail);
            
            // 3. Set what happens when you click it.
            holder.itemView.setOnClickListener(v -> {
                if (videoClickListener != null) videoClickListener.onVideoClick(item);
            });
        } else {
            // If it's a PHOTO:
            // 1. Hide the "Play" button icon.
            holder.ivPlayOverlay.setVisibility(View.GONE);
            
            // 2. Use the 'Glide' library to download and show the photo from the web.
            //    - placeholder: shows a generic icon while downloading.
            //    - error: shows an "X" icon if the download fails.
            //    - centerCrop: makes the image fit perfectly in the square.
            Glide.with(holder.itemView.getContext())
                    .load(item.getURI())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .centerCrop()
                    .into(holder.ivThumbnail);
            
            // 3. Set what happens when you click it.
            holder.itemView.setOnClickListener(v -> {
                if (imageClickListener != null) imageClickListener.onImageClick(item);
            });
        }
    }

    /**
     * --- 3. GET-ITEM-COUNT ---
     * Tells the app how many items are in the gallery.
     * 
     * Precondition: items is not null
     * Postcondition: Returns the size of the items list
     */
    @Override
    public int getItemCount() { return items.size(); }

    /**
     * --- THE VIEW HOLDER ---
     * This is a simple class that "holds" onto the UI parts of a single gallery item.
     * It keeps track of the image and the date text so we don't have to search for them 
     * every time the user scrolls. This makes the scrolling very fast and smooth.
     */
    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail, ivPlayOverlay;
        TextView  tvDate;

        /**
         * Precondition: itemView is not null and contains the expected view IDs
         * Postcondition: ViewHolder is initialized with references to the views in itemView
         */
        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link the Java variables to the IDs in our item_gallery.xml file.
            ivThumbnail   = itemView.findViewById(R.id.ivGalleryThumbnail);
            ivPlayOverlay = itemView.findViewById(R.id.ivPlayOverlay);
            tvDate        = itemView.findViewById(R.id.tvGalleryDate);
        }
    }

    /**
     * Helper method to turn a computer date (2024-05-10T12:00:00) 
     * into a human date (10 May 2024, 12:00).
     * 
     * @param isoDate The raw date from the database.
     * @return A pretty, readable date string.
     *
     * Precondition: isoDate is a String in ISO 8601 format or null
     * Postcondition: Returns a formatted date string or the original isoDate if parsing fails
     */
    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "Unknown date";
        try {
            // 1. Tell Java how to read the computer format.
            SimpleDateFormat parser    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date             date      = parser.parse(isoDate.substring(0, 19));
            
            // 2. Tell Java how we want the date to look for the user.
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            return formatter.format(date);
        } catch (ParseException e) {
            // If something goes wrong, just show the raw text rather than crashing.
            return isoDate;
        }
    }
}
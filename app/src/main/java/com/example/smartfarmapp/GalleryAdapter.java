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
 * 2-column grid adapter for FarmGallery items.
 *
 * - Photos: load via Glide → tap calls onImageClickListener
 * - Videos: show play-icon overlay → tap calls onVideoClickListener
 *
 * Requires Glide in build.gradle:
 *   implementation 'com.github.bumptech.glide:glide:4.16.0'
 *   annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    public interface OnImageClickListener { void onImageClick(FarmGallery item); }
    public interface OnVideoClickListener { void onVideoClick(FarmGallery item); }

    private final List<FarmGallery>    items;
    private final OnImageClickListener imageClickListener;
    private final OnVideoClickListener videoClickListener;

    public GalleryAdapter(List<FarmGallery> items,
                          OnImageClickListener imageClickListener,
                          OnVideoClickListener videoClickListener) {
        this.items              = items;
        this.imageClickListener = imageClickListener;
        this.videoClickListener = videoClickListener;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        FarmGallery item = items.get(position);
        holder.tvDate.setText(formatDate(item.getDate()));

        if (item.isVideo()) {
            holder.ivPlayOverlay.setVisibility(View.VISIBLE);
            // Use a generic placeholder for video cells
            Glide.with(holder.itemView.getContext())
                    .load(android.R.drawable.ic_media_play)
                    .into(holder.ivThumbnail);
            holder.itemView.setOnClickListener(v -> {
                if (videoClickListener != null) videoClickListener.onVideoClick(item);
            });
        } else {
            holder.ivPlayOverlay.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(item.getURI())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .centerCrop()
                    .into(holder.ivThumbnail);
            holder.itemView.setOnClickListener(v -> {
                if (imageClickListener != null) imageClickListener.onImageClick(item);
            });
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail, ivPlayOverlay;
        TextView  tvDate;
        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail  = itemView.findViewById(R.id.ivGalleryThumbnail);
            ivPlayOverlay = itemView.findViewById(R.id.ivPlayOverlay);
            tvDate       = itemView.findViewById(R.id.tvGalleryDate);
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "Unknown date";
        try {
            SimpleDateFormat parser    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date             date      = parser.parse(isoDate.substring(0, 19));
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            return formatter.format(date);
        } catch (ParseException e) {
            return isoDate;
        }
    }
}
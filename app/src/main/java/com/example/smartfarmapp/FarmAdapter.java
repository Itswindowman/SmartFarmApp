package com.example.smartfarmapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {

    private List<Farm> farmList;

    public FarmAdapter(List<Farm> farmList) {
        this.farmList = farmList;
    }

    @NonNull
    @Override
    public FarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_farm, parent, false);
        return new FarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmViewHolder holder, int position) {
        Farm farm = farmList.get(position);
        holder.tvTemp.setText("Temp: " + farm.getTemp() + "°C");
        holder.tvGroundHumid.setText("Ground Humidity: " + farm.getGroundHumid() + "%");
        holder.tvAirHumid.setText("Air Humidity: " + farm.getAirHumid() + "%");

        // Format date nicely
        String dateStr = farm.getDateTime();
        holder.tvDateTime.setText("Updated: " + formatDate(farm.getDateTime()));
    }

    @Override
    public int getItemCount() {
        return farmList.size();
    }

    static class FarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvTemp, tvGroundHumid, tvAirHumid, tvDateTime;

        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvGroundHumid = itemView.findViewById(R.id.tvGroundHumid);
            tvAirHumid = itemView.findViewById(R.id.tvAirHumid);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }



    }

    String formatDate(String isoDate) {
        try {
            // Supabase returns like "2025-11-30T10:15:30+00:00"
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(isoDate.substring(0,19)); // cut off timezone
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            return formatter.format(date);
        } catch (Exception e) {
            return isoDate; // fallback
        }
    }



}
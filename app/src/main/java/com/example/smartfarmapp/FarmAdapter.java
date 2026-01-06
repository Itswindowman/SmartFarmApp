package com.example.smartfarmapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {

    private List<Farm> farmList;
    private Vegetation activeVegetation; // The currently active vegetation to check ranges against
    private int defaultTextColor;

    public FarmAdapter(List<Farm> farmList) {
        this.farmList = farmList;
    }

    /**
     * Sets the active vegetation and notifies the adapter to redraw the list.
     * This will trigger onBindViewHolder for all visible items, reapplying the checks.
     * @param vegetation The vegetation whose ranges should be used for monitoring.
     */
    public void setActiveVegetation(Vegetation vegetation) {
        this.activeVegetation = vegetation;
        notifyDataSetChanged(); // Redraw the list with the new checks
    }

    @NonNull
    @Override
    public FarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_farm, parent, false);
        // Store the default text color once, when the ViewHolder is created.
        defaultTextColor = new TextView(parent.getContext()).getTextColors().getDefaultColor();
        return new FarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmViewHolder holder, int position) {
        Farm farm = farmList.get(position);

        // --- DATA BINDING ---
        holder.tvTemp.setText("Temp: " + farm.getTemp() + "°C");
        holder.tvGroundHumid.setText("Ground Humidity: " + farm.getGroundHumid() + "%");
        holder.tvAirHumid.setText("Air Humidity: " + farm.getAirHumid() + "%");
        holder.tvDateTime.setText("Updated: " + formatDate(farm.getDateTime()));

        // --- RANGE CHECKING LOGIC ---
        if (activeVegetation == null) {
            holder.tvTemp.setTextColor(defaultTextColor);
            holder.tvGroundHumid.setTextColor(defaultTextColor);
            holder.tvAirHumid.setTextColor(defaultTextColor);
            return;
        }

        boolean isDay = isDayTime(farm.getDateTime());

        // --- Check Temperature ---
        checkValue(holder.tvTemp, (double) farm.getTemp(), (double) (isDay ? activeVegetation.getDayTempMin() : activeVegetation.getNightTempMin()), (double) (isDay ? activeVegetation.getDayTempMax() : activeVegetation.getNightTempMax()));

        // --- Check Ground Humidity ---
        checkValue(holder.tvGroundHumid, (double) farm.getGroundHumid(), (double) (isDay ? activeVegetation.getDayGroundHumidMin() : activeVegetation.getNightGroundHumidMin()), (double) (isDay ? activeVegetation.getDayGroundHumidMax() : activeVegetation.getNightGroundHumidMax()));

        // --- Check Air Humidity ---
        checkValue(holder.tvAirHumid, (double) farm.getAirHumid(), (double) (isDay ? activeVegetation.getDayAirHumidMin() : activeVegetation.getNightAirHumidMin()), (double) (isDay ? activeVegetation.getDayAirHumidMax() : activeVegetation.getNightAirHumidMax()));
    }

    @Override
    public int getItemCount() {
        return farmList.size();
    }

    private void checkValue(TextView textView, Double value, Double min, Double max) {
        if (value == null || min == null || max == null) {
            textView.setTextColor(Color.GRAY);
            return;
        }
        if (value < min || value > max) {
            textView.setTextColor(Color.RED);
        } else {
            textView.setTextColor(defaultTextColor);
        }
    }

    private boolean isDayTime(String isoDate) {
        if (isoDate == null) return true;
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(isoDate.substring(0, 19));
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            return hour >= 6 && hour < 18;
        } catch (ParseException e) {
            e.printStackTrace();
            return true;
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null) return "N/A";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(isoDate.substring(0, 19));
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            return formatter.format(date);
        } catch (Exception e) {
            return isoDate;
        }
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
}

package com.example.smartfarmapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * --- ADAPTER EXPLANATION ---
 * An Adapter is a controller that sits between the RecyclerView (the visual list) and the data.
 * Its main jobs are:
 *  1. Create new row layouts when the list first appears.
 *  2. Take data for a specific position and "bind" it to a row layout.
 *  3. Tell the RecyclerView how many total items are in the list.
 *  4. Recycle row views as the user scrolls to save memory and be highly efficient.
 */
public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {

    private List<Farm> farmList;
    private Vegetation activeVegetation; // The currently active vegetation to check ranges against.
    private int defaultTextColor; // Stores the default text color to reset views when they are in range.

    public FarmAdapter(List<Farm> farmList) {
        this.farmList = farmList;
    }

    /**
     * This public method allows the MainFragment to tell the adapter which vegetation profile is currently active.
     * After setting the active vegetation, it calls `notifyDataSetChanged()` which tells the RecyclerView
     * to redraw itself, re-running the onBindViewHolder logic for all visible items.
     */
    public void setActiveVegetation(Vegetation vegetation) {
        this.activeVegetation = vegetation;
        notifyDataSetChanged();
    }

    /**
     * --- 1. Creating Views ---
     * This method is called by the RecyclerView when it needs a new row layout to display.
     * It only runs a few times at the beginning, creating just enough views to fill the screen plus a few extra for recycling.
     */
    @NonNull
    @Override
    public FarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // A LayoutInflater turns an XML layout file into an actual View object in code.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_farm, parent, false);
        
        // We get the default text color from a TextView's default state.
        // This is a reliable way to get the correct color for the current theme (light or dark).
        defaultTextColor = new TextView(parent.getContext()).getTextColors().getDefaultColor();
        
        return new FarmViewHolder(view);
    }

    /**
     * --- 2. Binding Data ---
     * This is the most important method. It's called every time a row needs to be shown, including when you scroll.
     * It takes a recycled ViewHolder and fills it with the data for the given `position`.
     */
    @Override
    public void onBindViewHolder(@NonNull FarmViewHolder holder, int position) {
        // Get the specific Farm object for this row from our list.
        Farm farm = farmList.get(position);

        // Set the text for each TextView in the row.
        holder.tvTemp.setText("Temp: " + farm.getTemp() + "°C");
        holder.tvGroundHumid.setText("Ground Humidity: " + farm.getGroundHumid() + "%");
        holder.tvAirHumid.setText("Air Humidity: " + farm.getAirHumid() + "%");
        holder.tvDateTime.setText("Updated: " + formatDate(farm.getDateTime()));

        // --- RANGE CHECKING LOGIC ---
        // If no vegetation profile is active, make sure all text is the default color and stop.
        if (activeVegetation == null) {
            holder.tvTemp.setTextColor(defaultTextColor);
            holder.tvGroundHumid.setTextColor(defaultTextColor);
            holder.tvAirHumid.setTextColor(defaultTextColor);
            return;
        }

        // Determine if the farm's timestamp corresponds to daytime or nighttime.
        boolean isDay = isDayTime(farm.getDateTime());

        // --- Check Temperature ---
        float tempMin, tempMax;
        if (isDay) {
            tempMin = activeVegetation.getDayTempMin();
            tempMax = activeVegetation.getDayTempMax();
        } else {
            tempMin = activeVegetation.getNightTempMin();
            tempMax = activeVegetation.getNightTempMax();
        }
        checkValue(holder.tvTemp, (double) farm.getTemp(), (double) tempMin, (double) tempMax);

        // --- Check Ground Humidity ---
        float groundHumidMin, groundHumidMax;
        if (isDay) {
            groundHumidMin = activeVegetation.getDayGroundHumidMin();
            groundHumidMax = activeVegetation.getDayGroundHumidMax();
        } else {
            groundHumidMin = activeVegetation.getNightGroundHumidMin();
            groundHumidMax = activeVegetation.getNightGroundHumidMax();
        }
        checkValue(holder.tvGroundHumid, (double) farm.getGroundHumid(), (double) groundHumidMin, (double) groundHumidMax);

        // --- Check Air Humidity ---
        float airHumidMin, airHumidMax;
        if (isDay) {
            airHumidMin = activeVegetation.getDayAirHumidMin();
            airHumidMax = activeVegetation.getDayAirHumidMax();
        } else {
            airHumidMin = activeVegetation.getNightAirHumidMin();
            airHumidMax = activeVegetation.getNightAirHumidMax();
        }
        checkValue(holder.tvAirHumid, (double) farm.getAirHumid(), (double) airHumidMin, (double) airHumidMax);
    }

    /**
     * --- 3. Item Count ---
     * A very simple method that just tells the RecyclerView the total number of items in the data list.
     */
    @Override
    public int getItemCount() {
        return farmList.size();
    }

    /**
     * A helper method to check if a sensor value is outside the allowed min/max range.
     * It sets the text color to RED for out-of-range values, GRAY for incomplete data,
     * and the default color for in-range values.
     */
    private void checkValue(TextView textView, Double value, Double min, Double max) {
        if (value == null || min == null || max == null) {
            textView.setTextColor(Color.GRAY);
        } else if (value < min || value > max) {
            textView.setTextColor(Color.RED);
        } else {
            textView.setTextColor(defaultTextColor);
        }
    }

    /**
     * A helper method to determine if a given timestamp is during the day (6:00 AM - 5:59 PM).
     */
    private boolean isDayTime(String isoDate) {
        if (isoDate == null) return true; // Default to daytime if no date is available
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(isoDate.substring(0, 19));
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            // Returns true if the hour is between 6 and 17 (inclusive).
            return hour >= 6 && hour < 18;
        } catch (ParseException e) {
            e.printStackTrace();
            return true; // Default to daytime on a parsing error
        }
    }

    /**
     * A helper method to format the technical date string from Supabase into a human-readable format.
     */
    private String formatDate(String isoDate) {
        if (isoDate == null) return "N/A";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(isoDate.substring(0, 19));
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            return formatter.format(date);
        } catch (Exception e) {
            return isoDate; // Fallback to the original string if formatting fails
        }
    }

    /**
     * --- THE VIEW HOLDER ---
     * A ViewHolder's job is to hold onto the Views that make up a single row (e.g., the TextViews).
     * The RecyclerView creates a few of these and then recycles them for new data as you scroll.
     * This is highly efficient because it avoids repeatedly calling `findViewById()`.
     */
    static class FarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvTemp, tvGroundHumid, tvAirHumid, tvDateTime;

        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            // We find the views by their ID here, just once.
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvGroundHumid = itemView.findViewById(R.id.tvGroundHumid);
            tvAirHumid = itemView.findViewById(R.id.tvAirHumid);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }
}

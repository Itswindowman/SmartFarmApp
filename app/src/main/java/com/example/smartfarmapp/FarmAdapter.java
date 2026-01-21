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

    // --- MEMBER VARIABLES ---
    private List<Farm> farmList; // This holds the list of data the adapter will display.
    private Vegetation activeVegetation; // The currently active vegetation profile to check sensor values against.
    private int defaultTextColor; // Stores the default text color to reset views when they are back in range.

    /**
     * --- CONSTRUCTOR ---
     * The constructor receives the initial list of farm data.
     * @param farmList The list of Farm objects to be displayed.
     */
    public FarmAdapter(List<Farm> farmList) {
        this.farmList = farmList;
    }

    /**
     * Allows the MainFragment to set the active vegetation profile.
     * After setting the profile, it calls `notifyDataSetChanged()` which tells the RecyclerView
     * to redraw itself, re-running the onBindViewHolder logic for all visible items to update their colors.
     * @param vegetation The new active vegetation profile.
     */
    public void setActiveVegetation(Vegetation vegetation) {
        this.activeVegetation = vegetation;
        notifyDataSetChanged(); // This is crucial to trigger a UI update.
    }

    /**
     * Returns the currently active vegetation profile.
     * @return The active Vegetation object.
     */
    public Vegetation getActiveVegetation() {
        return this.activeVegetation;
    }

    /**
     * --- 1. ON-CREATE-VIEW-HOLDER ---
     * This method is called by the RecyclerView only when it needs a brand new row layout to display.
     * It runs a few times at the beginning to create just enough views to fill the screen plus a few extra for recycling.
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new FarmViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public FarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // A LayoutInflater is a standard Android tool that turns an XML layout file into an actual View object in code.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_farm, parent, false);

        // We get the default text color from a newly created TextView. This is a reliable way
        // to get the correct color that matches the current theme (light or dark mode).
        defaultTextColor = new TextView(parent.getContext()).getTextColors().getDefaultColor();

        // Return a new ViewHolder instance containing the inflated view.
        return new FarmViewHolder(view);
    }

    /**
     * --- 2. ON-BIND-VIEW-HOLDER ---
     * This is the core method of the adapter. It's called every time a row needs to display data.
     * This happens when the list first loads and every time you scroll to a new item.
     * It takes a recycled ViewHolder and fills it with the correct data for the given `position`.
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull FarmViewHolder holder, int position) {
        // Get the specific Farm object for this row from our data list.
        Farm farm = farmList.get(position);

        // Set the text for each TextView in the row using the data from the Farm object.
        holder.tvTemp.setText("Temp: " + farm.getTemp() + "°C");
        holder.tvGroundHumid.setText("Ground Humidity: " + farm.getGroundHumid() + "%");
        holder.tvAirHumid.setText("Air Humidity: " + farm.getAirHumid() + "%");
        holder.tvDateTime.setText("Updated: " + formatDate(farm.getDateTime()));

        // --- DYNAMIC COLORING (RANGE CHECKING) LOGIC ---
        // If no vegetation profile is active, we can't check ranges. Reset all text to the default color and stop.
        if (activeVegetation == null) {
            holder.tvTemp.setTextColor(defaultTextColor);
            holder.tvGroundHumid.setTextColor(defaultTextColor);
            holder.tvAirHumid.setTextColor(defaultTextColor);
            return; // Exit the method early.
        }

        // Determine if the farm's timestamp corresponds to daytime or nighttime.
        boolean isDay = isDayTime(farm.getDateTime());

        // --- Check Temperature Range ---
        float tempMin, tempMax;
        if (isDay) {
            tempMin = activeVegetation.getDayTempMin();
            tempMax = activeVegetation.getDayTempMax();
        } else {
            tempMin = activeVegetation.getNightTempMin();
            tempMax = activeVegetation.getNightTempMax();
        }
        checkValue(holder.tvTemp, (double) farm.getTemp(), (double) tempMin, (double) tempMax);

        // --- Check Ground Humidity Range ---
        float groundHumidMin, groundHumidMax;
        if (isDay) {
            groundHumidMin = activeVegetation.getDayGroundHumidMin();
            groundHumidMax = activeVegetation.getDayGroundHumidMax();
        } else {
            groundHumidMin = activeVegetation.getNightGroundHumidMin();
            groundHumidMax = activeVegetation.getNightGroundHumidMax();
        }
        checkValue(holder.tvGroundHumid, (double) farm.getGroundHumid(), (double) groundHumidMin, (double) groundHumidMax);

        // --- Check Air Humidity Range ---
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
     * --- 3. GET-ITEM-COUNT ---
     * A very simple but essential method that just tells the RecyclerView the total number of items in the data list.
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return farmList.size();
    }

    /**
     * A helper method to check if a sensor value is outside the allowed min/max range.
     * It sets the text color to RED for out-of-range values and resets it to the default color for in-range values.
     * @param textView The TextView to color.
     * @param value The current sensor value.
     * @param min The minimum allowed value.
     * @param max The maximum allowed value.
     */
    private void checkValue(TextView textView, Double value, Double min, Double max) {
        // If any of the values are null (missing data), color the text gray to indicate uncertainty.
        if (value == null || min == null || max == null) {
            textView.setTextColor(Color.GRAY);
        } else if (value < min || value > max) {
            // If the value is outside the range, color it red to alert the user.
            textView.setTextColor(Color.RED);
        } else {
            // If the value is within the range, reset the color to the theme's default.
            textView.setTextColor(defaultTextColor);
        }
    }

    /**
     * A helper method to determine if a given timestamp is during the day (defined as 6:00 AM to 5:59 PM).
     * @param isoDate The date string from the database (in ISO 8601 format).
     * @return `true` if it's daytime, `false` otherwise.
     */
    public boolean isDayTime(String isoDate) {
        if (isoDate == null) return true; // Default to daytime if the date is missing.
        try {
            // The parser needs to match the format of the date string from the database.
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            // We parse only the main part of the date string, ignoring fractional seconds or timezone info.
            Date date = parser.parse(isoDate.substring(0, 19));
            // Use a Calendar object to easily extract the hour of the day.
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int hour = cal.get(Calendar.HOUR_OF_DAY); // Gets the hour in 24-hour format (0-23).
            // Returns true if the hour is between 6 and 17 (i.e., 6:00 AM to 5:59 PM).
            return hour >= 6 && hour < 18;
        } catch (ParseException e) {
            e.printStackTrace(); // Log the error if parsing fails.
            return true; // Default to daytime on a parsing error to be safe.
        }
    }

    /**
     * A helper method to format the technical date string from Supabase into a more human-readable format.
     * @param isoDate The date string from the database.
     * @return A formatted date string (e.g., "15 Jul 2024, 14:30").
     */
    private String formatDate(String isoDate) {
        if (isoDate == null) return "N/A"; // Handle missing date gracefully.
        try {
            // Same parsing logic as isDayTime.
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(isoDate.substring(0, 19));
            // Define the desired output format.
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            // Return the newly formatted string.
            return formatter.format(date);
        } catch (Exception e) {
            return isoDate; // If formatting fails for any reason, just return the original string.
        }
    }

    /**
     * --- THE VIEW HOLDER ---
     * A ViewHolder's job is to hold onto the references to the Views that make up a single row (e.g., the TextViews).
     * The RecyclerView creates a few of these and then recycles them, passing them to onBindViewHolder.
     * This is highly efficient because it completely avoids calling `findViewById()` repeatedly while scrolling, which is slow.
     */
    static class FarmViewHolder extends RecyclerView.ViewHolder {
        // Declare the views for a single row.
        TextView tvTemp, tvGroundHumid, tvAirHumid, tvDateTime;

        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            // We find the views by their ID here, just once when the ViewHolder is created.
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvGroundHumid = itemView.findViewById(R.id.tvGroundHumid);
            tvAirHumid = itemView.findViewById(R.id.tvAirHumid);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }
}

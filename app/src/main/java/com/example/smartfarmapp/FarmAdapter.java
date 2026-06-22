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
 *
 * Think of it as a bridge: on one side you have a List of data, and on the other side you have
 * the Screen. The Adapter crosses the bridge to pick up a data item and put it into a visual "box"
 * (the ViewHolder) so it can be shown to the user.
 */
public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {

    // --- MEMBER VARIABLES ---
    // This holds the actual data we want to show. It's a list of 'Farm' objects.
    private List<Farm> farmList; 
    
    // This profile contains the "ideal" temperature and humidity ranges. 
    // We use it to color the text red if a value is too high or too low.
    private Vegetation activeVegetation; 
    
    // We store the default text color (usually black or white depending on the theme)
    // so we can change the text back to normal if a sensor value returns to a safe range.
    private int defaultTextColor; 

    /**
     * --- CONSTRUCTOR ---
     * This is the "Setup" method. When we create this adapter in our Activity or Fragment,
     * we give it the list of farm data we want it to manage.
     * @param farmList The list of Farm objects to be displayed.
     *
     * Precondition: farmList is a valid List of Farm objects (can be empty).
     * Postcondition: A new FarmAdapter is created with the provided list.
     */
    public FarmAdapter(List<Farm> farmList) {
        this.farmList = farmList;
    }

    /**
     * Allows the MainFragment to set the active vegetation profile.
     * After setting the profile, it calls `notifyDataSetChanged()` which tells the RecyclerView
     * to redraw itself, re-running the onBindViewHolder logic for all visible items to update their colors.
     * @param vegetation The new active vegetation profile.
     *
     * Precondition: vegetation can be null or a valid Vegetation object.
     * Postcondition: activeVegetation is updated and the UI is notified to refresh.
     */
    public void setActiveVegetation(Vegetation vegetation) {
        this.activeVegetation = vegetation;
        // notifyDataSetChanged is like hitting the "Refresh" button. It tells the list
        // that the data or the rules (like ranges) have changed, so it should update the screen.
        notifyDataSetChanged(); 
    }

    /**
     * Returns the currently active vegetation profile.
     * @return The active Vegetation object.
     *
     * Precondition: None
     * Postcondition: Returns the current activeVegetation.
     */
    public Vegetation getActiveVegetation() {
        return this.activeVegetation;
    }

    /**
     * Clears all data from the list and removes the active vegetation profile.
     * Useful when logging out or refreshing the whole app state.
     * 
     * Precondition: None
     * Postcondition: farmList is cleared, activeVegetation is set to null, and the UI is notified to refresh.
     */
    public void clearData() {
        if (farmList != null) {
            farmList.clear();
        }
        activeVegetation = null;
        notifyDataSetChanged(); // Refresh the UI to show an empty list.
    }

    /**
     * --- 1. ON-CREATE-VIEW-HOLDER ---
     * This method is called by the RecyclerView only when it needs a brand new row layout to display.
     * It runs a few times at the beginning to create just enough views to fill the screen plus a few extra for recycling.
     * 
     * Imagine this as building the "Physical Box" that will hold our data.
     * 
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type (used if you have different types of rows).
     * @return A new FarmViewHolder that holds our inflated layout.
     *
     * Precondition: parent is not null and provides context.
     * Postcondition: A new FarmViewHolder is created with the inflated item_farm layout and defaultTextColor is initialized.
     */
    @NonNull
    @Override
    public FarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 'LayoutInflater' is like a 3D printer for XML files. 
        // It takes the XML layout 'item_farm' and creates a real View object from it.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_farm, parent, false);

        // We capture the default text color here once so we don't have to guess it later.
        // We create a temporary TextView just to read what color it normally uses.
        defaultTextColor = new TextView(parent.getContext()).getTextColors().getDefaultColor();

        // We put the new View inside a "ViewHolder" and return it.
        return new FarmViewHolder(view);
    }

    /**
     * --- 2. ON-BIND-VIEW-HOLDER ---
     * This is the core method of the adapter. It's called every time a row needs to display data.
     * This happens when the list first loads and every time you scroll to a new item.
     * 
     * Imagine this as "Filling the Box" with actual information.
     * 
     * @param holder The ViewHolder (the box) we are filling.
     * @param position The index of the item in our list (0, 1, 2...).
     *
     * Precondition: holder is not null and position is within the bounds of farmList.
     * Postcondition: The UI elements in the holder are updated with data from the Farm object at the given position, and colors are applied based on activeVegetation.
     */
    @Override
    public void onBindViewHolder(@NonNull FarmViewHolder holder, int position) {
        // Step 1: Get the data object for the current position.
        Farm farm = farmList.get(position);

        // Step 2: Set the text for each TextView using the data from the Farm object.
        holder.tvTemp.setText("Temp: " + farm.getTemp() + "°C");
        holder.tvGroundHumid.setText("Ground Humidity: " + farm.getGroundHumid() + "%");
        holder.tvAirHumid.setText("Air Humidity: " + farm.getAirHumid() + "%");
        // We use a helper method 'formatDate' to make the computer-style date look pretty for humans.
        holder.tvDateTime.setText("Updated: " + formatDate(farm.getDateTime()));

        // --- DYNAMIC COLORING (RANGE CHECKING) LOGIC ---
        // If the user hasn't selected a crop (Vegetation profile), we just use default colors.
        if (activeVegetation == null) {
            holder.tvTemp.setTextColor(defaultTextColor);
            holder.tvGroundHumid.setTextColor(defaultTextColor);
            holder.tvAirHumid.setTextColor(defaultTextColor);
            return; // Stop here and don't do any range checking.
        }

        // Determine if the reading was taken during Day or Night. 
        // Plants often have different temperature needs at night!
        boolean isDay = isDayTime(farm.getDateTime());

        // --- Check Temperature Range ---
        float tempMin, tempMax;
        if (isDay) {
            // Get daytime safety ranges
            tempMin = activeVegetation.getDayTempMin();
            tempMax = activeVegetation.getDayTempMax();
        } else {
            // Get nighttime safety ranges
            tempMin = activeVegetation.getNightTempMin();
            tempMax = activeVegetation.getNightTempMax();
        }
        // Use our helper to compare the actual value vs the min/max and set the color.
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
     * This tells the RecyclerView how many total rows it needs to prepare for.
     * @return The size of our data list.
     *
     * Precondition: farmList is not null.
     * Postcondition: Returns the size of farmList.
     */
    @Override
    public int getItemCount() {
        return farmList.size();
    }

    /**
     * A helper method to check if a sensor value is "Safe" or "Dangerous".
     * - RED: The value is outside the allowed range (too hot, too dry, etc.).
     * - GRAY: Data is missing.
     * - DEFAULT: Everything is fine.
     * 
     * @param textView The TextView we want to change the color of.
     * @param value The actual sensor reading.
     * @param min The lowest safe value.
     * @param max The highest safe value.
     *
     * Precondition: textView is not null.
     * Postcondition: Sets textView color to Color.GRAY if any numeric parameter is null, Color.RED if value is out of [min, max], or defaultTextColor if in range.
     */
    private void checkValue(TextView textView, Double value, Double min, Double max) {
        if (value == null || min == null || max == null) {
            // Missing data? Color it gray so the user knows it's uncertain.
            textView.setTextColor(Color.GRAY);
        } else if (value < min || value > max) {
            // Out of range? Color it RED to alert the farmer.
            textView.setTextColor(Color.RED);
        } else {
            // Safe? Use the normal text color.
            textView.setTextColor(defaultTextColor);
        }
    }

    /**
     * A helper method to determine if a given timestamp is during the day (6:00 AM to 5:59 PM).
     * We need this because many crops have different requirements for Day vs Night.
     * 
     * @param isoDate The date string (e.g., "2024-07-15T14:30:00").
     * @return `true` if it's daytime, `false` otherwise.
     *
     * Precondition: isoDate is a String in ISO 8601 format or null.
     * Postcondition: Returns true if the hour in isoDate is between 6 and 17 inclusive, false otherwise. Defaults to true.
     */
    public boolean isDayTime(String isoDate) {
        if (isoDate == null || isoDate.length() < 19) return true; // Safety check for bad data.
        try {
            // Create a parser that understands the "Year-Month-Day T Hour:Minute:Second" format.
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(isoDate.substring(0, 19));
            
            // Use a Calendar to look at the specific 'Hour' of that date.
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int hour = cal.get(Calendar.HOUR_OF_DAY); // Returns 0-23
            
            // We define Day as 6 AM to 6 PM (hour 6 to 17).
            return hour >= 6 && hour < 18;
        } catch (ParseException e) {
            e.printStackTrace(); 
            return true; // Default to daytime if we can't figure it out.
        }
    }

    /**
     * Formats the computer-friendly date from the database into something pretty for the user.
     * Converts "2024-07-15T14:30:00" -> "15 Jul 2024, 14:30"
     * 
     * @param isoDate The raw date string.
     * @return A readable date string.
     *
     * Precondition: isoDate is a String in ISO 8601 format or null.
     * Postcondition: Returns a formatted date string or the original isoDate if parsing fails.
     */
    private String formatDate(String isoDate) {
        if (isoDate == null) return "N/A";
        if (isoDate.length() < 19) return isoDate; 
        try {
            // 1. Parse the raw string into a Date object.
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(isoDate.substring(0, 19));
            
            // 2. Format that Date object into our desired style.
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            return formatter.format(date);
        } catch (Exception e) {
            return isoDate; // Return raw data if formatting fails.
        }
    }

    /**
     * --- THE VIEW HOLDER ---
     * A ViewHolder is like a "Container" for the views in a single row.
     * Instead of looking up 'tvTemp' every time we scroll (which is slow), 
     * we look it up once, save it here, and reuse it. This is why RecyclerView is so smooth!
     */
    static class FarmViewHolder extends RecyclerView.ViewHolder {
        // These variables hold the actual UI components for one row.
        TextView tvTemp, tvGroundHumid, tvAirHumid, tvDateTime;

        /**
         * The constructor for the ViewHolder.
         * @param itemView The entire row View (item_farm.xml).
         */
        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            // We find the views by their ID here, JUST ONCE.
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvGroundHumid = itemView.findViewById(R.id.tvGroundHumid);
            tvAirHumid = itemView.findViewById(R.id.tvAirHumid);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }
}

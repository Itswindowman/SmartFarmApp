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

// --- EXPLANATION ---
// A RecyclerView.Adapter is a controller object that sits between your RecyclerView
// and your data set (in this case, a List<Farm>).
// Its primary job is to create the necessary views for your data and to "bind" the data
// to those views as the user scrolls. It recycles views for efficiency, which is what
// makes RecyclerView so powerful for long lists.
public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {

    // This is the data set that the adapter will display.
    // It's a list of `Farm` objects.
    private List<Farm> farmList;

    // --- CONSTRUCTOR ---
    // The constructor for the adapter receives the data set (the list of farms).
    public FarmAdapter(List<Farm> farmList) {
        this.farmList = farmList;
    }

    // --- ADAPTER METHOD 1: onCreateViewHolder ---
    // This method is called by the RecyclerView when it needs to create a NEW row view.
    // This only happens a few times at the beginning. Once enough row views have been
    // created to fill the screen (and a few extra for recycling), this method is no longer called.
    @NonNull
    @Override
    public FarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // A `LayoutInflater` is an Android tool that can turn an XML layout file into
        // an actual `View` object in code.
        // Here, we are inflating our `item_farm.xml` layout file, which defines the UI for a single row.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_farm, parent, false);

        // We create a new `FarmViewHolder` to hold and manage the views inside our inflated layout.
        // The RecyclerView will hold on to this ViewHolder for recycling.
        return new FarmViewHolder(view);
    }

    // --- ADAPTER METHOD 2: onBindViewHolder ---
    // This method is called by the RecyclerView to display the data at a specific position.
    // This method is called repeatedly as the user scrolls. It takes a recycled ViewHolder
    // and fills it with the data for the requested row (position).
    @Override
    public void onBindViewHolder(@NonNull FarmViewHolder holder, int position) {
        // Get the `Farm` object from our list at the given position.
        Farm farm = farmList.get(position);

        // Use the `holder` to access the TextViews inside the row's layout and set their text.
        // We get the data from our `farm` object and display it.
        holder.tvTemp.setText("Temp: " + farm.getTemp() + "°C");
        holder.tvGroundHumid.setText("Ground Humidity: " + farm.getGroundHumid() + "%");
        holder.tvAirHumid.setText("Air Humidity: " + farm.getAirHumid() + "%");

        // The date from the database is often in a technical format (ISO 8601).
        // We use our helper function `formatDate` to turn it into a more human-readable string.
        holder.tvDateTime.setText("Updated: " + formatDate(farm.getDateTime()));
    }

    // --- ADAPTER METHOD 3: getItemCount ---
    // This method is very simple: it tells the RecyclerView the total number of items in our data set.
    // The RecyclerView uses this to know how far the user can scroll.
    @Override
    public int getItemCount() {
        // Our list's `size()` method gives us the total count of farms.
        return farmList.size();
    }

    // --- THE VIEW HOLDER ---
    // A ViewHolder is a simple but critical class. It acts as a wrapper around the View
    // for a single item in the list (our `item_farm.xml` layout).
    // Its only job is to hold references to the sub-views (the TextViews) inside the layout.
    // By doing this, we avoid calling `findViewById()` repeatedly, which is an expensive operation.
    // The RecyclerView recycles these ViewHolder objects.
    static class FarmViewHolder extends RecyclerView.ViewHolder {
        // Declare member variables for all the views inside our row layout that we want to change.
        TextView tvTemp, tvGroundHumid, tvAirHumid, tvDateTime;

        // The constructor for the ViewHolder receives the inflated view (`item_farm.xml`).
        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            // This is where we call `findViewById()` ONCE for each sub-view.
            // We find each TextView in the layout and store a reference to it in our variables.
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvGroundHumid = itemView.findViewById(R.id.tvGroundHumid);
            tvAirHumid = itemView.findViewById(R.id.tvAirHumid);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }

    /**
     * A helper function to parse a technical date string (ISO 8601 format)
     * and reformat it into a more user-friendly style.
     * @param isoDate The date string from the Supabase database (e.g., "2025-11-30T10:15:30+00:00").
     * @return A formatted date string (e.g., "30 Nov 2025, 10:15") or the original string if parsing fails.
     */
    String formatDate(String isoDate) {
        try {
            // Define a parser that understands the input format from Supabase.
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            // Parse the first 19 characters of the string to get a `Date` object, ignoring the timezone.
            Date date = parser.parse(isoDate.substring(0, 19));
            // Define a formatter for the desired output style.
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
            // Format the Date object into the new string and return it.
            return formatter.format(date);
        } catch (Exception e) {
            // If anything goes wrong during parsing (e.g., unexpected format), we log the error
            // and return the original, unchanged string as a fallback so the app doesn't crash.
            e.printStackTrace();
            return isoDate;
        }
    }
}

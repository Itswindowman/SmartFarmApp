package com.example.smartfarmapp;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import org.w3c.dom.Notation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainFragment extends Fragment {

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. You can now send notifications.
                    Toast.makeText(getContext(), "Notification permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied. Explain to the user why you need it.
                    Toast.makeText(getContext(), "Notifications will not be shown as permission was denied.", Toast.LENGTH_LONG).show();
                }
            });

    private Button btnHistory;
    private HistoryRepo historyRepo;



    public static final String CHANNEL_ID = "FarmAlerts";
    private RecyclerView recyclerView;
    private FarmAdapter adapter;
    private List<Farm> farmList;
    private FloatingActionButton fabAdd;
    private VegetationRepo vegetationRepo;
    private TextView tvActiveVegetation;
    private Button CameraBtn;

    private List<Vegetation> allVegetations = new ArrayList<>();
    private Vegetation selectedVegetation = null;
    private boolean isEditMode = false;

    public MainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        farmList = new ArrayList<>();
        adapter = new FarmAdapter(farmList);
        vegetationRepo = new VegetationRepo();


        // In onCreate() method, add:
        historyRepo = new HistoryRepo();


        createNotificationChannel();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showAddFarmDialog());

        recyclerView = view.findViewById(R.id.recyclerFarm);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        tvActiveVegetation = view.findViewById(R.id.tvActiveVegetation);
        Vegetation currentActiveProfile = adapter.getActiveVegetation();
        if (currentActiveProfile != null) {
            tvActiveVegetation.setText("Monitoring Profile: " + currentActiveProfile.getName());
        } else {
            tvActiveVegetation.setText("No active profile set");
        }


        btnHistory = view.findViewById(R.id.btnHistory);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                showHistoryDialog();
            });
        }

        CameraBtn = view.findViewById(R.id.CameraBtn);

        // Also update the CameraBtn to save to history when tapped:
        CameraBtn.setOnClickListener(v -> {
            // Instead of doing nothing, save current state to history
            saveCurrentStateToHistory();
        });

        askForNotificationPermission();
        loadFarmData();
        return view;
    }

    /**
     * Loads the active vegetation profile from SharedPreferences and sets it on the adapter.
     */
    private void loadActiveVegetationProfile() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", android.content.Context.MODE_PRIVATE);

        // 1. Get the saved JSON string. Default to null if not found.
        String vegetationJson = sharedPreferences.getString("active_vegetation_profile", null);

        if (vegetationJson != null) {
            // 2. If a saved profile exists, parse it back into a Vegetation object
            Gson gson = new Gson();
            Vegetation savedProfile = gson.fromJson(vegetationJson, Vegetation.class);

            // 3. Set it as the active profile on the adapter and update the UI text
            if (savedProfile != null) {
                adapter.setActiveVegetation(savedProfile);
                // We need to update tvActiveVegetation, but the view isn't created yet.
                // We will do that in onCreateView.
            }
        }
    }


    private void loadFarmData() {
        // 1. Get SharedPreferences instance
        // Use the correct android.content.Context
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", android.content.Context.MODE_PRIVATE);

        // 2. Read the saved FARM_ID. Use a default value of -1 to indicate it wasn't found.
        int userFarmId = sharedPreferences.getInt("farm_id", -1);

        // 3. Check if the farmId is valid before making a network call.
        if (userFarmId == -1) {
            Toast.makeText(getContext(), "Error: No Farm ID found for user.", Toast.LENGTH_LONG).show();
            // Use the correct, standard android.util.Log
            Log.e("MainFragment", "Could not load farm data, userFarmId is -1.");
            return; // Stop the method here because we can't fetch data without an ID
        }

        // 4. Call the updated fetchFarms method with the user's farm ID
        SupabaseService service = new SupabaseService();
        service.fetchFarms(userFarmId, new SupabaseService.FarmCallback() {
            @Override
            public void onSuccess(List<Farm> farms) {  // when internet request works
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        farmList.clear(); // Clear the old list
                        farmList.addAll(farms); // Add the new list (should contain just one farm)
                        adapter.notifyDataSetChanged(); // Update the RecyclerView

                        // Now run the notification check on the new data
                        checkForNotifications();
                    });
                }
            }

            @Override
            public void onFailure(Exception e) { // when internet request fails
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed to load farm data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }


    private void showAddFarmDialog() {

        // Setting up the "Screen" of the dialogView

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_farm, null);

        RadioGroup rgModeSelector = dialogView.findViewById(R.id.rgModeSelector);
        TextInputLayout tilFarmName = dialogView.findViewById(R.id.tilFarmName);
        EditText etFarmName = dialogView.findViewById(R.id.etFarmName);
        Spinner spinnerVegetation = dialogView.findViewById(R.id.spinnerVegetation);

        final EditText etDayTempMin = dialogView.findViewById(R.id.etDayTempMin);
        final EditText etDayTempMax = dialogView.findViewById(R.id.etDayTempMax);
        final EditText etNightTempMin = dialogView.findViewById(R.id.etNightTempMin);
        final EditText etNightTempMax = dialogView.findViewById(R.id.etNightTempMax);
        final EditText etDayGroundMin = dialogView.findViewById(R.id.etDayGroundMin);
        final EditText etDayGroundMax = dialogView.findViewById(R.id.etDayGroundMax);
        final EditText etNightGroundMin = dialogView.findViewById(R.id.etNightGroundMin);
        final EditText etNightGroundMax = dialogView.findViewById(R.id.etNightGroundMax);
        final EditText etDayAirMin = dialogView.findViewById(R.id.etDayAirMin);
        final EditText etDayAirMax = dialogView.findViewById(R.id.etDayAirMax);
        final EditText etNightAirMin = dialogView.findViewById(R.id.etNightAirMin);
        final EditText etNightAirMax = dialogView.findViewById(R.id.etNightAirMax);

        // an Array that stores all the fields
        final EditText[] allFields = {etDayTempMin, etDayTempMax, etNightTempMin, etNightTempMax,
                etDayGroundMin, etDayGroundMax, etNightGroundMin, etNightGroundMax, etDayAirMin, etDayAirMax, etNightAirMin, etNightAirMax};

        vegetationRepo.fetchVegetations(new VegetationRepo.FetchVegetationsCallback() { // Asybcronous Call to get the list of vegetations (happens in the background)
            @Override
            public void onSuccess(List<Vegetation> vegetations) {
                allVegetations = vegetations;

                // this is so the driodiwb cab display them
                List<String> vegetationNames = allVegetations.stream().map(Vegetation::getName).collect(Collectors.toList()); // Modern java way to convert a list of Veggies Objects to a simple list of strings (with just the names).

                // spinnerAdapter for the spinner with the names of the vegetations
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, vegetationNames);

                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // sets the adapter
                spinnerVegetation.setAdapter(spinnerAdapter);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Could not load existing vegetations.", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        /* this are 3 buttons
        the Reason for the null in positive/Neutral is the we put the logic later
        thats because we want to validate the data first (check for empty boxes) before we are closing
        if we did otherwise the Dialog will close before we can validate the data.
        so we set them to null and add the logic later
        * */
        builder.setNeutralButton("Set Active", null); // makes the vegetation active
        builder.setPositiveButton("Save", null); // saves data
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()); // dismiss stuff

        AlertDialog dialog = builder.create(); // Now the builder is done and the actual dialog is created.


        dialog.setOnShowListener(dialogInterface -> {
            Button btnNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            btnNeutral.setVisibility(View.GONE); // Hide it by default

            rgModeSelector.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbAddNew) {
                    isEditMode = false;
                    tilFarmName.setVisibility(View.VISIBLE);
                    spinnerVegetation.setVisibility(View.GONE);
                    btnNeutral.setVisibility(View.GONE); // Hide "Set Active" in add mode
                    clearForm(allFields, etFarmName);
                } else {
                    isEditMode = true;
                    tilFarmName.setVisibility(View.GONE);
                    spinnerVegetation.setVisibility(View.VISIBLE);
                    btnNeutral.setVisibility(View.VISIBLE); // Show "Set Active" in edit mode
                    if (!allVegetations.isEmpty()) {
                        spinnerVegetation.setSelection(0);
                        selectedVegetation = allVegetations.get(0);
                        populateForm(selectedVegetation, allFields);
                    }
                }
            });

            spinnerVegetation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedVegetation = allVegetations.get(position);
                    populateForm(selectedVegetation, allFields);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) { selectedVegetation = null; }
            });

            btnNeutral.setOnClickListener(v -> {
                if (selectedVegetation != null) {
                    tvActiveVegetation.setText("Monitoring Profile: " + selectedVegetation.getName());
                    adapter.setActiveVegetation(selectedVegetation);

                    // 1. Get SharedPrefs Editor
                    SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", android.content.Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    // Convert the vegetation Object to a JSON string
                    Gson gson = new Gson();
                    String vegetationJson = gson.toJson(selectedVegetation);

                    // 3. save the JSON string with a key
                    editor.putString("active_vegetation_profile", vegetationJson);
                    editor.apply();


                    //Trigger for notifactions
                    checkForNotifications();

                    Toast.makeText(getContext(), selectedVegetation.getName() + " is now the active monitoring profile.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Please select a vegetation first.", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                // Save logic remains the same
                 boolean isNameValid = true;
                if (!isEditMode) { // Only validate name field if in "Add New" mode
                    if (TextUtils.isEmpty(etFarmName.getText().toString())) {
                        etFarmName.setError("Name is required");
                        isNameValid = false;
                    }
                } else if (selectedVegetation == null) {
                    Toast.makeText(getContext(), "Please select a vegetation to edit.", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean allNumericFieldsFilled = true;
                for (EditText field : allFields) {
                    if (TextUtils.isEmpty(field.getText().toString())) {
                        field.setError("This field is required");
                        allNumericFieldsFilled = false;
                    }
                }

                if (!isNameValid || !allNumericFieldsFilled) {
                    Toast.makeText(getContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    Vegetation vegetationToSave = isEditMode ? selectedVegetation : new Vegetation();
                    if (isEditMode) {
                        vegetationToSave.setId(selectedVegetation.getId());
                    } else {
                        vegetationToSave.setName(etFarmName.getText().toString());
                    }

                    vegetationToSave.setDayTempMin(Float.parseFloat(etDayTempMin.getText().toString()));
                    vegetationToSave.setDayTempMax(Float.parseFloat(etDayTempMax.getText().toString()));
                    vegetationToSave.setNightTempMin(Float.parseFloat(etNightTempMin.getText().toString()));
                    vegetationToSave.setNightTempMax(Float.parseFloat(etNightTempMax.getText().toString()));
                    vegetationToSave.setDayGroundHumidMin(Float.parseFloat(etDayGroundMin.getText().toString()));
                    vegetationToSave.setDayGroundHumidMax(Float.parseFloat(etDayGroundMax.getText().toString()));
                    vegetationToSave.setNightGroundHumidMin(Float.parseFloat(etNightGroundMin.getText().toString()));
                    vegetationToSave.setNightGroundHumidMax(Float.parseFloat(etNightGroundMax.getText().toString()));
                    vegetationToSave.setDayAirHumidMin(Float.parseFloat(etDayAirMin.getText().toString()));
                    vegetationToSave.setDayAirHumidMax(Float.parseFloat(etDayAirMax.getText().toString()));
                    vegetationToSave.setNightAirHumidMin(Float.parseFloat(etNightAirMin.getText().toString()));
                    vegetationToSave.setNightAirHumidMax(Float.parseFloat(etNightAirMax.getText().toString()));

                    if (isEditMode) {
                        vegetationRepo.updateVegetation(vegetationToSave, new VegetationRepo.UpdateVegetationCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Vegetation updated!", Toast.LENGTH_SHORT).show();
                                loadFarmData();
                                dialog.dismiss();
                            }
                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        vegetationRepo.addVegetation(vegetationToSave, new VegetationRepo.AddVegetationCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Vegetation added!", Toast.LENGTH_SHORT).show();
                                loadFarmData();
                                dialog.dismiss();
                            }
                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Please ensure all numeric fields are valid.", Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void populateForm(Vegetation veg, EditText[] fields) {
        fields[0].setText(String.valueOf(veg.getDayTempMin()));
        fields[1].setText(String.valueOf(veg.getDayTempMax()));
        fields[2].setText(String.valueOf(veg.getNightTempMin()));
        fields[3].setText(String.valueOf(veg.getNightTempMax()));
        fields[4].setText(String.valueOf(veg.getDayGroundHumidMin()));
        fields[5].setText(String.valueOf(veg.getDayGroundHumidMax()));
        fields[6].setText(String.valueOf(veg.getNightGroundHumidMin()));
        fields[7].setText(String.valueOf(veg.getNightGroundHumidMax()));
        fields[8].setText(String.valueOf(veg.getDayAirHumidMin()));
        fields[9].setText(String.valueOf(veg.getDayAirHumidMax()));
        fields[10].setText(String.valueOf(veg.getNightAirHumidMin()));
        fields[11].setText(String.valueOf(veg.getNightAirHumidMax()));
    }

    private void clearForm(EditText[] fields, EditText nameField) {
        nameField.setText("");
        for (EditText field : fields) {
            field.setText("");
        }
    }

    public void createNotificationChannel(){
        // Create the NotificationChannel, but only on API 26+ (Android 8.0 Oreo)
        // The 'if' statement checks if the device's version is O or higher.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){ // CORRECTED
            CharSequence name = "Farm Alerts";
            String description = "Notification for when farm sensors are out of range";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    /**
     * Builds and displays the detailed notification on the user's device.
     */
    private void sendOutOfRangeNotification(String details, Vegetation activeProfile) {int icon = R.drawable.ic_launcher_foreground; // TODO: Replace with a real notification icon

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(requireContext(), CHANNEL_ID);
        } else {
            builder = new Notification.Builder(requireContext());
        }

        // --- IMPROVEMENT ---
        // Add the profile name to the title for better context
        builder.setSmallIcon(icon)
                .setContentTitle("Alert for Profile: " + activeProfile.getName())
                .setContentText(details)
                .setStyle(new Notification.BigTextStyle().bigText(details))
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true);
        // -------------------

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        notificationManager.notify(1, builder.build());
    }


    /**
     * Checks farm data against a profile and returns a detailed error string.
     */

    private String getOutOfRangeDetails(Farm farm, Vegetation profile)
    {
        if(profile == null || farm == null) return "";

        boolean isDay = adapter.isDayTime(farm.getDateTime());
        StringBuilder details = new StringBuilder();

            float tempMin;
            float tempMax;
            float groundHumidMin;
            float groundHumidMax;
            float airHumidMin;
            float airHumidMax;

            // Use a standard if/else block to set the min/max values
            if (isDay) {
                tempMin = profile.getDayTempMin();
                tempMax = profile.getDayTempMax();
                groundHumidMin = profile.getDayGroundHumidMin();
                groundHumidMax = profile.getDayGroundHumidMax();
                airHumidMin = profile.getDayAirHumidMin();
                airHumidMax = profile.getDayAirHumidMax();
            } else { // It's night
                tempMin = profile.getNightTempMin();
                tempMax = profile.getNightTempMax();
                groundHumidMin = profile.getNightGroundHumidMin();
                groundHumidMax = profile.getNightGroundHumidMax();
                airHumidMin = profile.getNightAirHumidMin();
                airHumidMax = profile.getNightAirHumidMax();
            }

            // Now check each value against the limits we just set

            // Check Temperature
            if (farm.getTemp() < tempMin) {
                details.append(String.format("Temp is too low by %.1f°C. ", tempMin - farm.getTemp()));
            } else if (farm.getTemp() > tempMax) {
                details.append(String.format("Temp is too high by %.1f°C. ", farm.getTemp() - tempMax));
            }

            // Check Ground Humidity
            if (farm.getGroundHumid() < groundHumidMin) {
                details.append(String.format("Ground Humid is too low by %.1f%%. ", groundHumidMin - farm.getGroundHumid()));
            } else if (farm.getGroundHumid() > groundHumidMax) {
                details.append(String.format("Ground Humid is too high by %.1f%%. ", farm.getGroundHumid() - groundHumidMax));
            }

            // Check Air Humidity
            if (farm.getAirHumid() < airHumidMin) {
                details.append(String.format("Air Humid is too low by %.1f%%. ", airHumidMin - farm.getAirHumid()));
            } else if (farm.getAirHumid() > airHumidMax) {
                details.append(String.format("Air Humid is too high by %.1f%%. ", farm.getAirHumid() - airHumidMax));
            }

            return details.toString().trim(); // .trim() removes any extra space at the end

    }

    private void checkForNotifications() {
        // 1. Get the current active profile from the adapter
        Vegetation activeProfile = adapter.getActiveVegetation();

        // 2. Check if we have an active profile and if there is data in our list
        if (activeProfile != null && !farmList.isEmpty()) {
            // 3. Get the most recent data entry
            Farm latestFarmData = farmList.get(0);

            // 4. Get a detailed report
            String details = getOutOfRangeDetails(latestFarmData, activeProfile);

            // 5. If the report is not empty, send the notification
            if(!details.isEmpty())
            {
                sendOutOfRangeNotification(details, activeProfile);
            }
        }
    }

    // --- ADD THIS METHOD ---
    private void askForNotificationPermission() {
        // This is only needed for Android 13 (TIRAMISU) and higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Permission is already granted.
            } else {
                // Directly ask for the permission.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void saveCurrentStateToHistory() {
        if (farmList.isEmpty()) {
            Toast.makeText(getContext(), "No farm data available", Toast.LENGTH_SHORT).show();
            return;
        }

        Farm latestFarm = farmList.get(0);
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        int farmId = sharedPreferences.getInt("farm_id", 1);

        // Make sure you're using the correct History constructor
        History history = new History(
                (long) farmId,
                (long) latestFarm.getTemp(),
                (long) latestFarm.getGroundHumid(),
                (long) latestFarm.getAirHumid()
        );

        Vegetation activeProfile = adapter.getActiveVegetation();
        if (activeProfile != null) {
            history.setNotes("Active profile: " + activeProfile.getName());
        }

        historyRepo.addHistory(history, new HistoryRepo.AddHistoryCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "✓ Farm state saved to history!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("MainFragment", "Save to history failed", e);
            }
        });
    }

    /**
     * Show history entries in a dialog - ONLY ONE VERSION!
     */
    private void showHistoryDialog() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        int farmId = sharedPreferences.getInt("farm_id", 1);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("History Log");
        builder.setMessage("Loading history...");
        builder.setCancelable(true);

        AlertDialog loadingDialog = builder.create();
        loadingDialog.show();

        // Use fetchFarmHistory for the FarmHistory table
        historyRepo.fetchFarmHistory((long) farmId, new HistoryRepo.FetchHistoryCallback() {
            @Override
            public void onSuccess(List<History> historyList) {
                loadingDialog.dismiss();

                if (historyList.isEmpty()) {
                    Toast.makeText(getContext(), "No history entries yet", Toast.LENGTH_SHORT).show();
                    return;
                }

                StringBuilder historyText = new StringBuilder();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

                for (int i = 0; i < Math.min(historyList.size(), 10); i++) {
                    History h = historyList.get(i);
                    // Use the NEW getter methods from the updated History class
                    // In showHistoryDialog() method:
                    historyText.append(sdf.format(h.getRecordedAt()))  // Make sure it's getRecordedAt() not getRecorded_at()
                            .append(" - Temp: ").append(h.getTemperature())
                            .append("°C, Ground: ").append(h.getGroundHumidity())
                            .append("%, Air: ").append(h.getAirHumidity())
                            .append("%\n");

                    if (h.getNotes() != null && !h.getNotes().isEmpty()) {
                        historyText.append("Notes: ").append(h.getNotes()).append("\n");
                    }
                    historyText.append("\n");
                }

                AlertDialog.Builder resultBuilder = new AlertDialog.Builder(requireContext());
                resultBuilder.setTitle("Recent History (" + historyList.size() + " entries)");
                resultBuilder.setMessage(historyText.toString());
                resultBuilder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                resultBuilder.setNeutralButton("Save Current", (dialog, which) -> {
                    saveCurrentStateToHistory();
                });
                resultBuilder.show();
            }

            @Override
            public void onFailure(Exception e) {
                loadingDialog.dismiss();
                Toast.makeText(getContext(), "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}







package com.example.smartfarmapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the main UI controller for your application's primary screen.
 * As a Fragment, it represents a portion of your user interface and manages all the user interaction
 * on this screen, such as displaying the list of farms, handling button clicks, and showing dialogs.
 */
public class MainFragment extends Fragment {

    // --- UI Elements ---
    private RecyclerView recyclerView; // Displays the scrollable list.
    private FloatingActionButton fabAdd; // The "+" button to add a new item.

    // --- Data & Adapter ---
    private FarmAdapter adapter; // The adapter that manages how data is shown in the RecyclerView.
    private List<Farm> farmList; // The list of data currently displayed. TODO: This should be updated to a List<Vegetation> and use a VegetationAdapter.

    // --- Logic & Data Source ---
    private VegetationRepo vegetationRepo; // The repository for all vegetation data operations.
    private List<Vegetation> allVegetations = new ArrayList<>(); // A cache of all vegetations from the DB, used for the dialog spinner.
    private Vegetation selectedVegetation = null; // Holds the vegetation object currently selected in the spinner.
    private boolean isEditMode = false; // A flag to track if the dialog is in "Add New" or "Edit Existing" mode.


    /**
     * --- CONSTRUCTOR ---
     * A public, no-argument constructor is required for all Fragments.
     * Android uses this to re-create the fragment when needed (e.g., on screen rotation).
     */
    public MainFragment() {}

    /**
     * --- FRAGMENT LIFECYCLE: onCreate ---
     * This is the first step in the fragment's life. It's called for non-UI initialization.
     * We set up our data lists and repository here, before the view is created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        farmList = new ArrayList<>();
        adapter = new FarmAdapter(farmList);
        vegetationRepo = new VegetationRepo(); // Create a single instance of the repository to be reused.
    }

    /**
     * --- FRAGMENT LIFECYCLE: onCreateView ---
     * This is where the fragment's UI is created and connected to the code.
     * It "inflates" the XML layout and wires up the UI components.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the XML layout file to create the View objects.
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Find the FloatingActionButton and set its click listener.
        fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showAddFarmDialog());

        // Find the RecyclerView and configure it.
        recyclerView = view.findViewById(R.id.recyclerFarm);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Arrange items in a vertical list.
        recyclerView.setAdapter(adapter); // Connect the RecyclerView to our adapter.

        // Now that the UI is ready, fetch the initial data to display.
        loadFarmData();
        return view;
    }

    /**
     * --- DATA LOADING ---
     * Fetches the list of vegetations from the database using the VegetationRepo.
     */
    private void loadFarmData() {
        // TODO: This is the next part of your code to update. It currently uses a placeholder Farm/SupabaseService.
        // It should be updated to use `vegetationRepo.fetchVegetations` and a proper `VegetationAdapter`.
        vegetationRepo.fetchVegetations(new VegetationRepo.FetchVegetationsCallback() {
            @Override
            public void onSuccess(List<Vegetation> vegetations) {
                if (isAdded()) { // Safety check: Make sure the fragment is still active.
                    allVegetations = vegetations;

                    // --- TEMPORARY WORKAROUND ---
                    // Your `FarmAdapter` expects a `List<Farm>`. This loop converts the fetched
                    // `List<Vegetation>` into a `List<Farm>` so the UI can display it.
                    // A better long-term solution is to create a `VegetationAdapter`.
                    farmList.clear();
                    for (Vegetation v : vegetations) {
                        Farm f = new Farm();
                        f.setFarmName(v.getName());
                        // You would map other fields here if your adapter needs them.
                        farmList.add(f);
                    }
                    adapter.notifyDataSetChanged(); // Tell the adapter the data has changed so it can refresh the list.
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    /**
     * --- DIALOG LOGIC ---
     * Creates, configures, and shows the complex "Add/Edit" dialog.
     */
    private void showAddFarmDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_farm, null);

        // --- Find all UI components from the dialog's layout ---
        RadioGroup rgModeSelector = dialogView.findViewById(R.id.rgModeSelector);
        TextInputLayout tilFarmName = dialogView.findViewById(R.id.tilFarmName);
        EditText etFarmName = dialogView.findViewById(R.id.etFarmName);
        Spinner spinnerVegetation = dialogView.findViewById(R.id.spinnerVegetation);

        final EditText etDayTempMin = dialogView.findViewById(R.id.etDayTempMin);
        final EditText etDayTempMax = dialogView.findViewById(R.id.etDayTempMax);
        // ... (find all other EditText fields)
        final EditText[] allFields = {etDayTempMin, etDayTempMax, /* ... all other numeric fields */ };

        // --- Initial State Setup: Fetch data for the spinner ---
        vegetationRepo.fetchVegetations(new VegetationRepo.FetchVegetationsCallback() {
            @Override
            public void onSuccess(List<Vegetation> vegetations) {
                allVegetations = vegetations;
                List<String> vegetationNames = allVegetations.stream().map(Vegetation::getName).collect(Collectors.toList());
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, vegetationNames);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerVegetation.setAdapter(spinnerAdapter);
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Could not load existing vegetations for spinner.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- UI Logic for Mode Switching (Add New vs. Select Existing) ---
        rgModeSelector.setOnCheckedChangeListener((group, checkedId) -> {
            isEditMode = (checkedId == R.id.rbSelectExisting);
            tilFarmName.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
            spinnerVegetation.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

            if (isEditMode && !allVegetations.isEmpty()) {
                // Default to the first item when switching to edit mode
                spinnerVegetation.setSelection(0);
                populateForm(allVegetations.get(0), allFields);
            } else {
                clearForm(allFields, etFarmName);
            }
        });

        // --- Spinner Item Selection Logic ---
        spinnerVegetation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // When a user selects an item from the spinner, populate the form with its data.
                selectedVegetation = allVegetations.get(position);
                populateForm(selectedVegetation, allFields);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedVegetation = null;
                clearForm(allFields, etFarmName);
            }
        });

        // --- Dialog Builder and Display ---
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        // We set the buttons to null here so we can override the click listener later.
        // This allows us to perform validation without automatically closing the dialog.
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {

                // --- FORM VALIDATION ---
                // (Validation logic to check if all fields are filled correctly)

                // --- SAVE OR UPDATE LOGIC ---
                try {
                    // Create the Vegetation object from the form data.
                    Vegetation vegetationToSave = buildVegetationFromForm(isEditMode, selectedVegetation, etFarmName, allFields);

                    if (isEditMode) {
                        // UPDATE EXISTING RECORD
                        vegetationRepo.updateVegetation(vegetationToSave, new VegetationRepo.UpdateVegetationCallback() {
                            @Override public void onSuccess() { /* Show success, reload, dismiss */ }
                            @Override public void onFailure(Exception e) { /* Show error */ }
                        });
                    } else {
                        // ADD NEW RECORD
                        vegetationRepo.addVegetation(vegetationToSave, new VegetationRepo.AddVegetationCallback() {
                            @Override public void onSuccess() { /* Show success, reload, dismiss */ }
                            @Override public void onFailure(Exception e) { /* Show error */ }
                        });
                    }
                } catch (NumberFormatException e) {
                    // This will catch errors if the user enters non-numeric text in a number field.
                    Toast.makeText(getContext(), "Please enter valid numbers.", Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * --- HELPER METHODS for the Dialog ---
     */

    // Populates the EditText fields with data from a Vegetation object.
    private void populateForm(Vegetation veg, EditText[] fields) {
        // Null-safe way to set text. If a value is null, it sets an empty string.
        fields[0].setText(veg.getDayTempMin() != null ? String.valueOf(veg.getDayTempMin()) : "");
        // ... (set text for all other fields)
    }

    // Clears all text from the form fields.
    private void clearForm(EditText[] fields, EditText nameField) {
        nameField.setText("");
        for (EditText field : fields) {
            field.setText("");
        }
    }

    // Builds a Vegetation object from the current data in the form fields.
    private Vegetation buildVegetationFromForm(boolean isEditMode, Vegetation selectedVeg, EditText nameField, EditText[] numericFields) throws NumberFormatException {
        Vegetation veg = isEditMode ? selectedVeg : new Vegetation();
        if (isEditMode) {
            veg.setId(selectedVeg.getId());
        } else {
            veg.setName(nameField.getText().toString());
        }
        veg.setDayTempMin(Double.parseDouble(numericFields[0].getText().toString()));
        // ... (parse and set all other numeric fields)
        return veg;
    }
}

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

public class MainFragment extends Fragment {

    private RecyclerView recyclerView;
    private FarmAdapter adapter;
    private List<Farm> farmList;
    private FloatingActionButton fabAdd;
    private VegetationRepo vegetationRepo;

    // New member variables for the dialog logic
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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showAddFarmDialog());

        recyclerView = view.findViewById(R.id.recyclerFarm);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadFarmData();
        return view;
    }

    private void loadFarmData() {
        // This method should be updated to use VegetationRepo and a VegetationAdapter
        SupabaseService service = new SupabaseService();
        service.fetchFarms(new SupabaseService.FarmCallback() {
            @Override
            public void onSuccess(List<Farm> farms) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        farmList.clear();
                        farmList.addAll(farms);
                        adapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void showAddFarmDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_farm, null);

        // Find all UI components
        RadioGroup rgModeSelector = dialogView.findViewById(R.id.rgModeSelector);
        RadioButton rbAddNew = dialogView.findViewById(R.id.rbAddNew);
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

        final EditText[] allFields = {etDayTempMin, etDayTempMax, etNightTempMin, etNightTempMax,
                etDayGroundMin, etDayGroundMax, etNightGroundMin, etNightGroundMax, etDayAirMin, etDayAirMax, etNightAirMin, etNightAirMax};


        // --- Initial State Setup ---
        // Fetch all vegetations to populate the spinner later.
        vegetationRepo.fetchVegetations(new VegetationRepo.FetchVegetationsCallback() {
            @Override
            public void onSuccess(List<Vegetation> vegetations) {
                allVegetations = vegetations;
                // Get just the names for the spinner
                List<String> vegetationNames = allVegetations.stream().map(Vegetation::getName).collect(Collectors.toList());
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, vegetationNames);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerVegetation.setAdapter(spinnerAdapter);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Could not load existing vegetations.", Toast.LENGTH_SHORT).show();
            }
        });


        // --- UI Logic for Mode Switching ---
        rgModeSelector.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAddNew) {
                isEditMode = false;
                tilFarmName.setVisibility(View.VISIBLE);
                spinnerVegetation.setVisibility(View.GONE);
                clearForm(allFields, etFarmName);
            } else {
                isEditMode = true;
                tilFarmName.setVisibility(View.GONE);
                spinnerVegetation.setVisibility(View.VISIBLE);
                // If there are vegetations, select the first one by default
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


        // --- Dialog Builder ---
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {

                // --- Validation ---
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

                // --- Save or Update ---
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
                        // UPDATE EXISTING
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
                        // ADD NEW
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

    // Helper method to populate the form with data from a selected vegetation
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

    // Helper method to clear all form fields
    private void clearForm(EditText[] fields, EditText nameField) {
        nameField.setText("");
        for (EditText field : fields) {
            field.setText("");
        }
    }
}

package com.example.smartfarmapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {

    // RecyclerView + Adapter
    private RecyclerView recyclerView;
    private FarmAdapter adapter;
    private List<Farm> farmList;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize list here so it's ready before view creation
        farmList = new ArrayList<>();
        adapter = new FarmAdapter(farmList);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Setup Floating Action Button
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Toast.makeText(getContext(), "FAB clicked!", Toast.LENGTH_SHORT).show();
            // TODO: Add your action (open new fragment, add record, etc.)
        });

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recyclerFarm);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Fetch data from Supabase
        loadFarmData();

        return view;
    }

    /**
     * Fetch farm data from Supabase and update RecyclerView.
     */
    private void loadFarmData() {
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
                e.printStackTrace();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
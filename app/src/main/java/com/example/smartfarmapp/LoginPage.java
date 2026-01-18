package com.example.smartfarmapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.textfield.TextInputLayout;

public class LoginPage extends Fragment {

    private UserRepo userRepo;
    private EditText etEmail, etPassword;
    private CheckBox cbRememberMe;
    private Button btnSignIn, btnSignUp;

    // Constants for SharedPreferences
    private static final String PREFS_NAME = "SmartFarmPrefs";
    private static final String PREF_EMAIL = "email";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_REMEMBER = "remember";

    public LoginPage() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepo = new UserRepo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find all UI elements
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        cbRememberMe = view.findViewById(R.id.cbRememberMe);
        btnSignIn = view.findViewById(R.id.btnSignIn);
        btnSignUp = view.findViewById(R.id.btnSignUp);

        // Load saved preferences
        loadPreferences();

        // Set click listeners
        btnSignIn.setOnClickListener(v -> handleSignIn());
        btnSignUp.setOnClickListener(v -> handleSignUp());
    }

    private void handleSignIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        // Save preferences if "Remember Me" is checked
        savePreferences(email, password, cbRememberMe.isChecked());

        // Use the UserRepo to sign in
        userRepo.getUser(email, password, new UserRepo.GetUserCallback() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(getContext(), "Sign In Successful!", Toast.LENGTH_SHORT).show();
                // Navigate to the MainFragment
                NavHostFragment.findNavController(LoginPage.this)
                        .navigate(R.id.action_loginPage_to_mainFragment);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Sign In Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSignUp() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        // Create a new user object
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(password);
        // You can set other default values for a new user here if needed

        // Use the UserRepo to add the new user
        userRepo.addUser(newUser, new UserRepo.AddUserCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Sign Up Successful! Please sign in.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Sign Up Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return false;
        }
        return true;
    }

    /**
     * Saves user preferences (email, password, remember me choice) to SharedPreferences.
     */
    private void savePreferences(String email, String password, boolean remember) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (remember) {
            editor.putString(PREF_EMAIL, email);
            editor.putString(PREF_PASSWORD, password);
            editor.putBoolean(PREF_REMEMBER, true);
        } else {
            // If "Remember Me" is unchecked, clear the saved credentials.
            editor.clear();
        }
        editor.apply();
    }

    /**
     * Loads saved user preferences from SharedPreferences and populates the fields.
     */
    private void loadPreferences() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean rememberMe = sharedPreferences.getBoolean(PREF_REMEMBER, false);
        cbRememberMe.setChecked(rememberMe);
        if (rememberMe) {
            etEmail.setText(sharedPreferences.getString(PREF_EMAIL, ""));
            etPassword.setText(sharedPreferences.getString(PREF_PASSWORD, ""));
        }
    }
}

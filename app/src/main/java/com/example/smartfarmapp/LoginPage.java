package com.example.smartfarmapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

/**
 * --- FRAGMENT EXPLANATION ---
 * This Fragment manages the Login screen. Its responsibilities are:
 * - Displaying the UI for email/password input.
 * - Handling user input for signing in and signing up.
 * - Communicating with the `UserRepo` to authenticate or create users.
 * - Remembering user credentials if "Remember Me" is checked.
 * - Navigating to the main part of the app upon successful login.
 */
public class LoginPage extends Fragment {

    // --- UI ELEMENT DECLARATIONS ---
    // These variables will hold references to the views defined in your XML layout file.
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private CheckBox cbRememberMe;
    private Button btnSignIn;
    private Button btnSignUp;

    // --- SharedPreferences FOR "REMEMBER ME" ---
    // SharedPreferences is a simple way to save small key-value pairs of data, perfect for user settings.
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "SmartFarmPrefs"; // A unique name for your app's preference file.
    private static final String KEY_EMAIL = "email"; // Key for storing the user's email.
    private static final String KEY_PASSWORD = "password"; // Key for storing the user's password.
    private static final String KEY_REMEMBER_ME = "remember_me"; // Key for storing the "Remember Me" checkbox state.
    private static final String FARM_ID = "farm_id"; // Key for storing the user's associated farm ID.

    // --- DATA REPOSITORY ---
    // This holds the reference to the UserRepo, which handles all user data operations.
    private UserRepo userRepo;

    /**
     * Required empty public constructor for the Android framework.
     */
    public LoginPage() {}

    /**
     * --- ON-CREATE-VIEW ---
     * This method is called when the fragment needs to create its UI for the first time.
     * It "inflates" the XML layout file into actual View objects.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment and return the root view.
        return inflater.inflate(R.layout.fragment_login_page, container, false);
    }

    /**
     * --- ON-VIEW-CREATED ---
     * This method is called immediately after `onCreateView` has finished.
     * It's the perfect place to find your views, set up listeners, and initialize data.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- INITIALIZATION ---
        // Create an instance of the user repository.
        userRepo = new UserRepo();
        // Get the SharedPreferences instance for your app.
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // --- FIND UI ELEMENTS ---
        // Link the variables to the actual views in the layout using their IDs.
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        cbRememberMe = view.findViewById(R.id.cbRememberMe);
        btnSignIn = view.findViewById(R.id.btnSignIn);
        btnSignUp = view.findViewById(R.id.btnSignUp);

        // --- LOAD SAVED PREFERENCES ---
        // Check if the user previously selected "Remember Me" and fill in the fields if so.
        loadPreferences();

        // --- SET UP CLICK LISTENERS ---
        // Assign actions to be performed when the buttons are clicked.
        btnSignIn.setOnClickListener(v -> handleSignIn());
        btnSignUp.setOnClickListener(v -> handleSignUp());
    }

    /**
     * Loads the "Remember Me" settings from SharedPreferences and updates the UI accordingly.
     */
    private void loadPreferences() {
        // Get the saved boolean value for KEY_REMEMBER_ME. Default to `false` if not found.
        boolean shouldRemember = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        cbRememberMe.setChecked(shouldRemember);

        // If the user should be remembered, retrieve and set the saved email and password.
        if (shouldRemember) {
            etEmail.setText(sharedPreferences.getString(KEY_EMAIL, ""));
            etPassword.setText(sharedPreferences.getString(KEY_PASSWORD, ""));
        }
    }

    /**
     * Handles the logic for the Sign In button click.
     */
    private void handleSignIn() {
        // Get the text from the input fields and remove any leading/trailing whitespace.
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();

        // Basic validation: ensure fields are not empty.
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Email and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return; // Stop the process.
        }

        // Call the repository to attempt to sign the user in.
        userRepo.getUser(email, password, new UserRepo.GetUserCallback() {
            @Override
            public void onSuccess(User user) {
                // This code runs if the repository successfully finds a matching user.
                Toast.makeText(getContext(), "Sign In Successful!", Toast.LENGTH_SHORT).show();

                // Get the editor to save data to SharedPreferences.
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (cbRememberMe.isChecked()) {
                    // If "Remember Me" is checked, save the credentials and farm ID.
                    editor.putBoolean(KEY_REMEMBER_ME, true);
                    editor.putString(KEY_EMAIL, email);
                    editor.putString(KEY_PASSWORD, password); // Note: Storing passwords in plain text is insecure.
                    editor.putInt(FARM_ID, user.getFarmID());
                } else {
                    // If not checked, clear any previously saved credentials.
                    editor.remove(KEY_EMAIL);
                    editor.remove(KEY_PASSWORD);
                    editor.putBoolean(KEY_REMEMBER_ME, false);
                }
                editor.apply(); // Apply the changes to SharedPreferences.

                // Use the Navigation Component to move to the main screen of the app.
                NavHostFragment.findNavController(LoginPage.this)
                        .navigate(R.id.action_loginPage_to_mainFragment);
            }

            @Override
            public void onFailure(Exception e) {
                // This code runs if the repository reports a failure (e.g., wrong password).
                Toast.makeText(getContext(), "Sign In Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Handles the logic for the Sign Up button click.
     */
    private void handleSignUp() {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();

        // Basic validation.
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Email and Password cannot be empty for Sign Up", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new User object with the provided details.
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(password); // WARNING: Passwords should be hashed before being stored!
        newUser.setFarmID(1); // Using a placeholder Farm ID.
        newUser.setLatitude(0.0f); // Placeholder
        newUser.setLongitude(0.0f); // Placeholder

        // Call the repository to add the new user to the database.
        userRepo.addUser(newUser, new UserRepo.AddUserCallback() {
            @Override
            public void onSuccess() {
                // On success, inform the user and clear the password field for them to sign in.
                Toast.makeText(getContext(), "Sign Up Successful! Please Sign In.", Toast.LENGTH_LONG).show();
                etPassword.setText(""); // Clear password field for convenience.
            }

            @Override
            public void onFailure(Exception e) {
                // On failure, show an error message (e.g., user already exists).
                Toast.makeText(getContext(), "Sign Up Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

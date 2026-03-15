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

public class LoginPage extends Fragment {

    // ── UI ────────────────────────────────────────────────────────────────────
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private CheckBox          cbRememberMe;
    private Button            btnSignIn;
    private Button            btnSignUp;

    // ── SharedPreferences keys ────────────────────────────────────────────────
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME      = "SmartFarmPrefs";
    private static final String KEY_EMAIL       = "email";
    private static final String KEY_PASSWORD    = "password";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_USER_ID     = "user_id";   // ← NEW: store the DB id

    // ── Repo ──────────────────────────────────────────────────────────────────
    private UserRepo userRepo;

    public LoginPage() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepo          = new UserRepo();
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        etEmail      = view.findViewById(R.id.etEmail);
        etPassword   = view.findViewById(R.id.etPassword);
        cbRememberMe = view.findViewById(R.id.cbRememberMe);
        btnSignIn    = view.findViewById(R.id.btnSignIn);
        btnSignUp    = view.findViewById(R.id.btnSignUp);

        loadPreferences();

        btnSignIn.setOnClickListener(v -> handleSignIn());
        btnSignUp.setOnClickListener(v -> handleSignUp());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void loadPreferences() {
        boolean shouldRemember = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        cbRememberMe.setChecked(shouldRemember);
        if (shouldRemember) {
            etEmail.setText(sharedPreferences.getString(KEY_EMAIL, ""));
            etPassword.setText(sharedPreferences.getString(KEY_PASSWORD, ""));
        }
    }

    private void handleSignIn() {
        String email    = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Email and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        userRepo.getUser(email, password, new UserRepo.GetUserCallback() {
            @Override
            public void onSuccess(User user) {
                // ── BUG FIX: save the real DB id so MainFragment and Gallery can use it ──
                SharedPreferences.Editor editor = sharedPreferences.edit();

                if (user.getId() != null) {
                    editor.putInt(KEY_USER_ID, user.getId().intValue());
                } else {
                    // Fallback: should never happen, but guard against it
                    Toast.makeText(getContext(),
                            "Sign In error: user has no ID. Contact support.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (cbRememberMe.isChecked()) {
                    editor.putBoolean(KEY_REMEMBER_ME, true);
                    editor.putString(KEY_EMAIL, email);
                    editor.putString(KEY_PASSWORD, password);
                } else {
                    editor.remove(KEY_EMAIL);
                    editor.remove(KEY_PASSWORD);
                    editor.putBoolean(KEY_REMEMBER_ME, false);
                }
                editor.apply();

                Toast.makeText(getContext(), "Sign In Successful!", Toast.LENGTH_SHORT).show();

                NavHostFragment.findNavController(LoginPage.this)
                        .navigate(R.id.action_loginPage_to_mainFragment);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(),
                        "Sign In Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSignUp() {
        String email    = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(),
                    "Email and Password cannot be empty for Sign Up", Toast.LENGTH_SHORT).show();
            return;
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setLatitude(0.0f);
        newUser.setLongitude(0.0f);

        userRepo.addUser(newUser, new UserRepo.AddUserCallback() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(getContext(),
                        "Sign Up Successful! Please Sign In.", Toast.LENGTH_LONG).show();
                etPassword.setText("");
            }

            @Override
            public void onFailure(Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("409")) {
                    Toast.makeText(getContext(),
                            "Sign Up Failed: User already exists.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(),
                            "Sign Up Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
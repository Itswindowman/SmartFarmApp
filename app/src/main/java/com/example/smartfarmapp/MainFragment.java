package com.example.smartfarmapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import android.content.BroadcastReceiver;


public class MainFragment extends Fragment {

    // Add these fields with the other member variables
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private AlertDialog noNetworkDialog;          // reference to the currently shown dialog
    private ImageButton btnLogout; // Add this for the logout button

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Logout Button
        btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> handleLogout());

        // FAB
        fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            if (NetworkUtil.isInternetAvailable(requireContext())) {
                showAddFarmDialog();
            } else {
                Toast.makeText(getContext(), "No internet connection. Cannot add farm.", Toast.LENGTH_SHORT).show();
            }
        });

        // RecyclerView
        recyclerView = view.findViewById(R.id.recyclerFarm);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Active vegetation label
        tvActiveVegetation = view.findViewById(R.id.tvActiveVegetation);
        Vegetation currentActiveProfile = adapter.getActiveVegetation();
        if (currentActiveProfile != null) {
            tvActiveVegetation.setText("Monitoring Profile: " + currentActiveProfile.getName());
        } else {
            tvActiveVegetation.setText("No active profile set");
        }

        // ── CHANGED: History button → Gallery ─────────────────────────────────
        btnGallery = view.findViewById(R.id.btnGallery);
        if (btnGallery != null) {
            btnGallery.setText("Gallery");                        // rename label
            btnGallery.setOnClickListener(v -> {
                if (NetworkUtil.isInternetAvailable(requireContext())) {
                    showGalleryDialog();
                } else {
                    Toast.makeText(getContext(), "No internet connection. Cannot open gallery.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // LiveCameraBtn – UNCHANGED, still opens the WebView camera stream
        LiveCameraBtn = view.findViewById(R.id.LiveCameraBtn);
        LiveCameraBtn.setOnClickListener(v -> {
            if (NetworkUtil.isInternetAvailable(requireContext())) {
                showLiveCameraDialog();
            } else {
                Toast.makeText(getContext(), "No internet connection. Camera stream unavailable.", Toast.LENGTH_SHORT).show();
            }
        });

        // Daily reminder switch – UNCHANGED
        setupDailyReminderSwitch(view);

        // Permissions & initial data
        askForNotificationPermission();
        loadFarmData();
        loadActiveVegetationFromDB();   // CHANGED: DB instead of SharedPreferences

        return view;
    }

    private void handleLogout() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("remember_me", false);
        editor.remove("email");
        editor.remove("password");
        editor.remove("user_id");
        editor.apply();

        if (adapter != null) {
            adapter.clearData();
        }

        Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();

        NavHostFragment.findNavController(MainFragment.this)
                .navigate(R.id.action_mainFragment_to_loginPage);
    }

    // ... (rest of the file is unchanged)
    // ─────────────────────────────────────────────────────────────────────────
    // PERMISSION LAUNCHER  (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Notification permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Notifications will not be shown as permission was denied.", Toast.LENGTH_LONG).show();
                }
            });

    // ─────────────────────────────────────────────────────────────────────────
    // MEDIA-PICKER LAUNCHERS  (new – for the Gallery dialog)
    // Must be registered here (before onStart), NOT inside a method.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Holds the UploadCallback set by showGalleryDialog() right before the picker is launched.
     * Consumed (set to null) after use to prevent double-firing.
     */
    private FarmGalleryRepo.UploadCallback pendingUploadCallback;

    private final ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> handlePickerResult(result, "image/*"));

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> handlePickerResult(result, "video/*"));

    // ─────────────────────────────────────────────────────────────────────────
    // BROADCAST RECEIVER  (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainFragment", "📡 Received broadcast: Data updated!");
            loadFarmData();
        }
    };

    // ─────────────────────────────────────────────────────────────────────────
    // ORIGINAL UI REFERENCES
    // ─────────────────────────────────────────────────────────────────────────

    private Button               btnGallery;      // renamed to "Gallery" at runtime
    public static final String   CHANNEL_ID = "FarmAlerts";
    private RecyclerView         recyclerView;
    private FarmAdapter          adapter;
    private List<Farm>           farmList;
    private FloatingActionButton fabAdd;
    private VegetationRepo       vegetationRepo;
    private TextView             tvActiveVegetation;
    private Button               LiveCameraBtn;   // ← unchanged; still opens camera stream

    // ─────────────────────────────────────────────────────────────────────────
    // ORIGINAL STATE
    // ─────────────────────────────────────────────────────────────────────────

    private List<Vegetation> allVegetations    = new ArrayList<>();
    private Vegetation       selectedVegetation = null;
    private boolean          isEditMode         = false;

    private int       FarmTimer    = 1000;
    private Timer     refreshTimer;
    private TimerTask refreshTask;

    // ─────────────────────────────────────────────────────────────────────────
    // NEW FIELDS
    // ─────────────────────────────────────────────────────────────────────────

    private FarmGalleryRepo    galleryRepo;
    private UserVegetationRepo userVegetationRepo;

    /** Live reference to the gallery grid adapter so we can refresh it after an upload */
    private GalleryAdapter    galleryGridAdapter;
    private List<FarmGallery> galleryItems = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────────

    public MainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        farmList           = new ArrayList<>();
        adapter            = new FarmAdapter(farmList);
        vegetationRepo     = new VegetationRepo();
        galleryRepo        = new FarmGalleryRepo();        // NEW
        userVegetationRepo = new UserVegetationRepo();     // NEW
        createNotificationChannel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // onResume / onPause  (structure unchanged; vegetation now loaded from DB)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();

        // Start network monitoring (shows dialog when offline)
        setupNetworkMonitoring();

        startPeriodicRefresh();

        Log.d("MainFragment", "onResume – registering receiver");
        Log.d("MainFragment", "🟢 Fragment resumed - Setting up monitoring");

        IntentFilter filter = new IntentFilter(FarmMonitoringService.ACTION_DATA_UPDATED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(dataUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(dataUpdateReceiver, filter);
        }
        Log.d("MainFragment", "✅ BroadcastReceiver registered");

        Intent serviceIntent = new Intent(requireContext(), FarmMonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
        Log.d("MainFragment", "✅ Monitoring service started");

        loadFarmData();
        loadActiveVegetationFromDB();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopPeriodicRefresh();

        Log.d("MainFragment", "onPause – unregistering receiver");
        Log.d("MainFragment", "🟡 Fragment paused - Unregistering receiver");

        try {
            requireActivity().unregisterReceiver(dataUpdateReceiver);
            Log.d("MainFragment", "✅ BroadcastReceiver unregistered");
        } catch (IllegalArgumentException e) {
            Log.w("MainFragment", "Receiver was not registered");
        }

        // Unregister network callback to prevent leaks
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }

        // Dismiss any open dialog
        if (noNetworkDialog != null && noNetworkDialog.isShowing()) {
            noNetworkDialog.dismiss();
            noNetworkDialog = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEW: LOAD ACTIVE VEGETATION FROM DB
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Queries UserVegetation table for the most recent row belonging to this user,
     * then fetches the full Vegetation record and applies it as the active profile.
     * Falls back to SharedPreferences on failure.
     */
    private void loadActiveVegetationFromDB() {
        if (!NetworkUtil.isInternetAvailable(requireContext())) {
            Log.d("MainFragment", "loadActiveVegetationFromDB: Skipping, no internet.");
            loadActiveVegetationFallback();
            return;
        }

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            Log.w("MainFragment", "loadActiveVegetationFromDB: no user_id saved");
            return;
        }

        userVegetationRepo.fetchActiveVegetation(userId,
                new UserVegetationRepo.ActiveVegetationCallback() {
                    @Override
                    public void onSuccess(Vegetation vegetation) {
                        if (!isAdded()) return;
                        if (vegetation != null) {
                            Log.d("MainFragment", "Active veg from DB: " + vegetation.getName());
                            adapter.setActiveVegetation(vegetation);
                            tvActiveVegetation.setText("Monitoring Profile: " + vegetation.getName());
                            // Persist for FarmMonitoringService
                            Gson gson = new Gson();
                            String json = gson.toJson(vegetation);
                            prefs.edit()
                                    .putString("active_vegetation",         json)
                                    .putString("active_vegetation_profile", json)
                                    .apply();
                        } else {
                            Log.d("MainFragment", "No UserVegetation row found – using prefs fallback");
                            loadActiveVegetationFallback();
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Log.e("MainFragment", "DB veg load failed – using prefs fallback: " + e.getMessage());
                        loadActiveVegetationFallback();
                    }
                });
    }

    /** Fallback: read from SharedPreferences (old behavior). */
    private void loadActiveVegetationFallback() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("active_vegetation_profile", null);
        if (json != null) {
            Vegetation saved = new Gson().fromJson(json, Vegetation.class);
            if (saved != null && isAdded()) {
                adapter.setActiveVegetation(saved);
                tvActiveVegetation.setText("Monitoring Profile: " + saved.getName());
                prefs.edit().putString("active_vegetation", json).apply();
                Log.d("MainFragment", "Loaded active veg from prefs: " + saved.getName());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEW: GALLERY DIALOG
    // ─────────────────────────────────────────────────────────────────────────


    /**
     *
     * Layout: dialog_gallery.xml
     *   • Photo button → phone image picker → upload → refresh grid
     *   • Video button → phone video picker → upload → refresh grid
     *   • Status text view for live upload feedback
     *   • 2-column RecyclerView grid of all items for this user
     *
     * Image tap → showFullscreenImageDialog()
     * Video tap → showFullscreenVideoDialog()
     */
    private void showGalleryDialog() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            Toast.makeText(getContext(), "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_gallery, null);

        Button   btnPickPhoto   = dialogView.findViewById(R.id.btnPickPhoto);
        Button   btnPickVideo   = dialogView.findViewById(R.id.btnPickVideo);
        TextView tvUploadStatus = dialogView.findViewById(R.id.tvUploadStatus);
        RecyclerView rvGallery  = dialogView.findViewById(R.id.rvGallery);

        // Set up 2-column grid
        rvGallery.setLayoutManager(new GridLayoutManager(getContext(), 2));
        galleryItems       = new ArrayList<>();
        galleryGridAdapter = new GalleryAdapter(
                galleryItems,
                item -> showFullscreenImageDialog(item.getURI()),   // photo tap
                item -> showFullscreenVideoDialog(item.getURI())    // video tap
        );
        rvGallery.setAdapter(galleryGridAdapter);

        AlertDialog galleryDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Close", (d, w) -> d.dismiss())
                .create();
        galleryDialog.show();

        // Load existing items immediately
        tvUploadStatus.setText("Loading gallery…");
        loadGalleryItems(userId, tvUploadStatus);

        // ── Photo picker button ───────────────────────────────────────────────
        btnPickPhoto.setOnClickListener(v -> {
            if (!NetworkUtil.isInternetAvailable(requireContext())) {
                tvUploadStatus.setText("❌ No internet connection.");
                return;
            }
            tvUploadStatus.setText("Select a photo from your gallery…");
            pendingUploadCallback = new FarmGalleryRepo.UploadCallback() {
                @Override public void onSuccess(String url) {
                    tvUploadStatus.setText("✅ Photo uploaded!");
                    loadGalleryItems(userId, tvUploadStatus);
                }
                @Override public void onFailure(Exception e) {
                    tvUploadStatus.setText("❌ Upload failed: " + e.getMessage());
                    Log.e("MainFragment", "Photo upload failed", e);
                }
            };
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            photoPickerLauncher.launch(intent);
        });

        // ── Video picker button ───────────────────────────────────────────────
        btnPickVideo.setOnClickListener(v -> {
            if (!NetworkUtil.isInternetAvailable(requireContext())) {
                tvUploadStatus.setText("❌ No internet connection.");
                return;
            }
            tvUploadStatus.setText("Select a video from your gallery…");
            pendingUploadCallback = new FarmGalleryRepo.UploadCallback() {
                @Override public void onSuccess(String url) {
                    tvUploadStatus.setText("✅ Video uploaded!");
                    loadGalleryItems(userId, tvUploadStatus);
                }
                @Override public void onFailure(Exception e) {
                    tvUploadStatus.setText("❌ Upload failed: " + e.getMessage());
                    Log.e("MainFragment", "Video upload failed", e);
                }
            };
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType("video/*");
            videoPickerLauncher.launch(intent);
        });
    }

    /** Called by both picker launchers after the user selects a file. */
    private void handlePickerResult(ActivityResult result, String fallbackMime) {
        if (result.getResultCode() != Activity.RESULT_OK
                || result.getData() == null
                || result.getData().getData() == null) {
            Log.d("MainFragment", "Picker cancelled or returned no data");
            return;
        }

        Uri fileUri = result.getData().getData();
        String mimeType = requireContext().getContentResolver().getType(fileUri);
        if (mimeType == null) mimeType = fallbackMime;

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        if (userId == -1 || pendingUploadCallback == null) {
            Toast.makeText(getContext(), "Upload error – please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtil.isInternetAvailable(requireContext())) {
            Toast.makeText(getContext(), "No internet connection. Upload aborted.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Uploading… please wait", Toast.LENGTH_SHORT).show();
        galleryRepo.uploadAndSave(requireContext(), fileUri, mimeType, userId, pendingUploadCallback);
        pendingUploadCallback = null; // consumed
    }

    /** Fetches the user's gallery rows from Supabase and refreshes the grid. */
    private void loadGalleryItems(int userId, TextView statusView) {
        if (!NetworkUtil.isInternetAvailable(requireContext())) {
            statusView.setText("❌ No internet connection.");
            return;
        }

        galleryRepo.fetchGalleryForUser(userId, new FarmGalleryRepo.FetchGalleryCallback() {
            @Override public void onSuccess(List<FarmGallery> items) {
                if (!isAdded()) return;
                galleryItems.clear();
                galleryItems.addAll(items);
                galleryGridAdapter.notifyDataSetChanged();
                statusView.setText(items.isEmpty()
                        ? "No photos or videos yet – upload one above!"
                        : items.size() + " item(s)");
            }
            @Override public void onFailure(Exception e) {
                if (!isAdded()) return;
                statusView.setText("Failed to load gallery: " + e.getMessage());
                Log.e("MainFragment", "loadGalleryItems failed", e);
            }
        });
    }

    /** Shows a photo full-screen using Glide inside an AlertDialog. */
    private void showFullscreenImageDialog(String imageUrl) {
        View imgView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_fullscreen_image, null);
        ImageView ivFull = imgView.findViewById(R.id.ivFullscreenImage);
        Glide.with(requireContext()).load(imageUrl).into(ivFull);

        new AlertDialog.Builder(requireContext())
                .setView(imgView)
                .setPositiveButton("Close", (d, w) -> d.dismiss())
                .show();
    }

    /**
     * Shows a video full-screen in a WebView inside an AlertDialog.
     * The video URL is embedded in an HTML5 <video> tag so it plays inline.
     */
    private void showFullscreenVideoDialog(String videoUrl) {
        View videoView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_fullscreen_video, null);
        WebView wv = videoView.findViewById(R.id.wvVideo);

        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setMediaPlaybackRequiresUserGesture(false);
        wv.setWebViewClient(new WebViewClient());

        // Wrap in HTML5 <video> – works for any direct-link mp4 / webm
        String html = "<html><body style='margin:0;padding:0;background:#000;'>"
                + "<video width='100%' height='100%' controls autoplay playsinline>"
                + "<source src='" + videoUrl + "'>"
                + "Your browser does not support this video format."
                + "</video></body></html>";
        wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(videoView)
                .setPositiveButton("Close", (d, w) -> {
                    wv.loadUrl("about:blank"); // stop playback
                    wv.destroy();
                    d.dismiss();
                })
                .create();

        dialog.show();

        // Make dialog fill screen width
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width  = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIVE CAMERA DIALOG  – PRESERVED EXACTLY FROM ORIGINAL
    // ─────────────────────────────────────────────────────────────────────────

    private void showLiveCameraDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_live_camera, null);

        WebView        webView       = dialogView.findViewById(R.id.webView);
        View           loadingOverlay = dialogView.findViewById(R.id.loadingOverlay);
        TextView       statusText    = dialogView.findViewById(R.id.statusText);
        ImageView      btnClose      = dialogView.findViewById(R.id.btnClose);
        MaterialButton btnFullscreen = dialogView.findViewById(R.id.btnFullscreen);
        MaterialButton btnSnapshot   = dialogView.findViewById(R.id.btnSnapshot);
        MaterialButton btnRecord     = dialogView.findViewById(R.id.btnRecord);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loadingOverlay.setVisibility(View.GONE);
                statusText.setVisibility(View.VISIBLE);
            }
        });
        // Replace with your actual camera URL
        webView.loadUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSrOunpvF6cRJnnrnfpFksyRMhWcQyJfivzAQ&s");

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnFullscreen.setOnClickListener(v -> {
            try {
                ViewGroup.LayoutParams params = webView.getLayoutParams();
                if (params instanceof ConstraintLayout.LayoutParams) {
                    ConstraintLayout.LayoutParams cp = (ConstraintLayout.LayoutParams) params;
                    if (cp.height == ConstraintLayout.LayoutParams.MATCH_PARENT) {
                        cp.height = 0;
                        cp.dimensionRatio = "H,4:3";
                        btnFullscreen.setText("Fullscreen");
                        Log.d("LiveCamera", "Exited fullscreen mode");
                    } else {
                        cp.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
                        cp.dimensionRatio = null;
                        btnFullscreen.setText("Exit Fullscreen");
                        Log.d("LiveCamera", "Entered fullscreen mode");
                    }
                    webView.setLayoutParams(cp);
                } else {
                    Log.e("LiveCamera", "Parent is not ConstraintLayout! It's: " +
                            params.getClass().getSimpleName());
                    Toast.makeText(getContext(), "Fullscreen not supported in this layout",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("LiveCamera", "Fullscreen error: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Error toggling fullscreen", Toast.LENGTH_SHORT).show();
            }
        });

        btnSnapshot.setOnClickListener(v ->
                Toast.makeText(getContext(), "Snapshot saved!", Toast.LENGTH_SHORT).show());

        btnRecord.setOnClickListener(v -> {
            if (btnRecord.getText().toString().equals("REC")) {
                btnRecord.setText("STOP");
                btnRecord.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.red)));
                Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();
            } else {
                btnRecord.setText("REC");
                btnRecord.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.green)));
                Toast.makeText(getContext(), "Recording saved", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width  = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVERYTHING BELOW IS UNCHANGED FROM ORIGINAL
    // ─────────────────────────────────────────────────────────────────────────

    private void setupDailyReminderSwitch(View view) {
        Switch switchDailyReminder = view.findViewById(R.id.switchDailyReminder);
        boolean isEnabled = SimpleAlarmManager.isDailyReminderEnabled(requireContext());
        switchDailyReminder.setChecked(isEnabled);
        switchDailyReminder.setOnCheckedChangeListener((buttonView, isChecked) ->
                SimpleAlarmManager.setDailyReminder(requireContext(), isChecked));
    }

    private void askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void loadFarmData() {
        if (!NetworkUtil.isInternetAvailable(requireContext())) {
            Log.d("MainFragment", "loadFarmData: Skipping periodic load, no internet.");
            return;
        }

        Log.d("MainFragment", "─────────────────────────────────────────");
        Log.d("MainFragment", "🔄 loadFarmData() STARTED");

        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        int userId = sharedPreferences.getInt("user_id", -1);
        Log.d("MainFragment", "User ID: " + userId);

        if (userId == -1) {
            Toast.makeText(getContext(), "Error: No Farm ID found for user.", Toast.LENGTH_LONG).show();
            Log.e("MainFragment", "Could not load farm data, userFarmId is -1.");
            return;
        }

        SupabaseService service = new SupabaseService();
        service.fetchFarms(userId, new SupabaseService.FarmCallback() {
            @Override
            public void onSuccess(List<Farm> farms) {
                if (getActivity() == null || !isAdded()) {
                    Log.e("MainFragment", "❌ Cannot update - fragment not attached");
                    return;
                }
                Log.d("MainFragment", "✅ Fragment attached - updating UI");
                getActivity().runOnUiThread(() -> {
                    farmList.clear();
                    farmList.addAll(farms);
                    adapter.notifyDataSetChanged();
                    recyclerView.post(() -> adapter.notifyDataSetChanged());
                    recyclerView.invalidate();
                    recyclerView.requestLayout();
                    Log.d("MainFragment", "✅ UI UPDATE COMPLETED!");
                });
            }
            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Failed to load farm data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showAddFarmDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_farm, null);

        RadioGroup      rgModeSelector    = dialogView.findViewById(R.id.rgModeSelector);
        TextInputLayout tilFarmName       = dialogView.findViewById(R.id.tilFarmName);
        EditText        etFarmName        = dialogView.findViewById(R.id.etFarmName);
        Spinner         spinnerVegetation = dialogView.findViewById(R.id.spinnerVegetation);
        final EditText  etDayTempMin      = dialogView.findViewById(R.id.etDayTempMin);
        final EditText  etDayTempMax      = dialogView.findViewById(R.id.etDayTempMax);
        final EditText  etNightTempMin    = dialogView.findViewById(R.id.etNightTempMin);
        final EditText  etNightTempMax    = dialogView.findViewById(R.id.etNightTempMax);
        final EditText  etDayGroundMin    = dialogView.findViewById(R.id.etDayGroundMin);
        final EditText  etDayGroundMax    = dialogView.findViewById(R.id.etDayGroundMax);
        final EditText  etNightGroundMin  = dialogView.findViewById(R.id.etNightGroundMin);
        final EditText  etNightGroundMax  = dialogView.findViewById(R.id.etNightGroundMax);
        final EditText  etDayAirMin       = dialogView.findViewById(R.id.etDayAirMin);
        final EditText  etDayAirMax       = dialogView.findViewById(R.id.etDayAirMax);
        final EditText  etNightAirMin     = dialogView.findViewById(R.id.etNightAirMin);
        final EditText  etNightAirMax     = dialogView.findViewById(R.id.etNightAirMax);

        final EditText[] allFields = {etDayTempMin, etDayTempMax, etNightTempMin, etNightTempMax,
                etDayGroundMin, etDayGroundMax, etNightGroundMin, etNightGroundMax,
                etDayAirMin, etDayAirMax, etNightAirMin, etNightAirMax};

        vegetationRepo.fetchVegetations(new VegetationRepo.FetchVegetationsCallback() {
            @Override public void onSuccess(List<Vegetation> vegetations) {
                allVegetations = vegetations;
                List<String> vegetationNames = allVegetations.stream()
                        .map(Vegetation::getName).collect(Collectors.toList());
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, vegetationNames);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerVegetation.setAdapter(spinnerAdapter);
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Could not load existing vegetations.", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        builder.setNeutralButton("Set Active", null);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            btnNeutral.setVisibility(View.GONE);

            rgModeSelector.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbAddNew) {
                    isEditMode = false;
                    tilFarmName.setVisibility(View.VISIBLE);
                    spinnerVegetation.setVisibility(View.GONE);
                    btnNeutral.setVisibility(View.GONE);
                    clearForm(allFields, etFarmName);
                } else {
                    isEditMode = true;
                    tilFarmName.setVisibility(View.GONE);
                    spinnerVegetation.setVisibility(View.VISIBLE);
                    btnNeutral.setVisibility(View.VISIBLE);
                    if (!allVegetations.isEmpty()) {
                        spinnerVegetation.setSelection(0);
                        selectedVegetation = allVegetations.get(0);
                        populateForm(selectedVegetation, allFields);
                    }
                }
            });

            spinnerVegetation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedVegetation = allVegetations.get(position);
                    populateForm(selectedVegetation, allFields);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { selectedVegetation = null; }
            });

            btnNeutral.setOnClickListener(v -> {
                if (selectedVegetation != null) {
                    if (!NetworkUtil.isInternetAvailable(requireContext())) {
                        Toast.makeText(getContext(), "No internet connection. Cannot set profile.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    tvActiveVegetation.setText("Monitoring Profile: " + selectedVegetation.getName());
                    adapter.setActiveVegetation(selectedVegetation);

                    SharedPreferences sharedPreferences = requireActivity()
                            .getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
                    Gson gson = new Gson();
                    String vegetationJson = gson.toJson(selectedVegetation);
                    sharedPreferences.edit()
                            .putString("active_vegetation_profile", vegetationJson)
                            .putString("active_vegetation", vegetationJson)
                            .apply();

                    checkForNotifications();
                    Toast.makeText(getContext(),
                            selectedVegetation.getName() + " is now the active monitoring profile.",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Please select a vegetation first.", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                if (!NetworkUtil.isInternetAvailable(requireContext())) {
                    Toast.makeText(getContext(), "No internet connection. Cannot save.", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isNameValid = true;
                if (!isEditMode) {
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
                            @Override public void onSuccess(Void result) {
                                Toast.makeText(getContext(), "Vegetation updated!", Toast.LENGTH_SHORT).show();
                                loadFarmData();
                                dialog.dismiss();
                            }
                            @Override public void onFailure(Exception e) {
                                Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        vegetationRepo.addVegetation(vegetationToSave, new VegetationRepo.AddVegetationCallback() {
                            @Override public void onSuccess(Void result) {
                                Toast.makeText(getContext(), "Vegetation added!", Toast.LENGTH_SHORT).show();
                                loadFarmData();
                                dialog.dismiss();
                            }
                            @Override public void onFailure(Exception e) {
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
        for (EditText field : fields) field.setText("");
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Farm Alerts";
            String description = "Notification for when farm sensors are out of range";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            requireActivity().getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }
    }

    private void sendOutOfRangeNotification(String details, Vegetation activeProfile) {
        int icon = R.drawable.ic_launcher_foreground;
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(requireContext(), CHANNEL_ID);
        } else {
            builder = new Notification.Builder(requireContext());
        }
        builder.setSmallIcon(icon)
                .setContentTitle("Alert for Profile: " + activeProfile.getName())
                .setContentText(details)
                .setStyle(new Notification.BigTextStyle().bigText(details))
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true);
        requireContext().getSystemService(NotificationManager.class).notify(1, builder.build());
    }

    private String getOutOfRangeDetails(Farm farm, Vegetation profile) {
        if (profile == null || farm == null) return "";
        boolean isDay = adapter.isDayTime(farm.getDateTime());
        StringBuilder details = new StringBuilder();
        float tempMin, tempMax, groundHumidMin, groundHumidMax, airHumidMin, airHumidMax;
        if (isDay) {
            tempMin = profile.getDayTempMin();         tempMax = profile.getDayTempMax();
            groundHumidMin = profile.getDayGroundHumidMin(); groundHumidMax = profile.getDayGroundHumidMax();
            airHumidMin = profile.getDayAirHumidMin(); airHumidMax = profile.getDayAirHumidMax();
        } else {
            tempMin = profile.getNightTempMin();         tempMax = profile.getNightTempMax();
            groundHumidMin = profile.getNightGroundHumidMin(); groundHumidMax = profile.getNightGroundHumidMax();
            airHumidMin = profile.getNightAirHumidMin(); airHumidMax = profile.getNightAirHumidMax();
        }
        if (farm.getTemp() < tempMin)
            details.append(String.format(Locale.US, "Temp is too low by %.1f°C. ", tempMin - farm.getTemp()));
        else if (farm.getTemp() > tempMax)
            details.append(String.format(Locale.US, "Temp is too high by %.1f°C. ", farm.getTemp() - tempMax));
        if (farm.getGroundHumid() < groundHumidMin)
            details.append(String.format(Locale.US, "Ground Humid is too low by %.1f%%. ", groundHumidMin - farm.getGroundHumid()));
        else if (farm.getGroundHumid() > groundHumidMax)
            details.append(String.format(Locale.US, "Ground Humid is too high by %.1f%%. ", farm.getGroundHumid() - groundHumidMax));
        if (farm.getAirHumid() < airHumidMin)
            details.append(String.format(Locale.US, "Air Humid is too low by %.1f%%. ", airHumidMin - farm.getAirHumid()));
        else if (farm.getAirHumid() > airHumidMax)
            details.append(String.format(Locale.US, "Air Humid is too high by %.1f%%. ", farm.getAirHumid() - airHumidMax));
        return details.toString().trim();
    }

    private void checkForNotifications() {
        Vegetation activeProfile = adapter.getActiveVegetation();
        if (activeProfile != null && !farmList.isEmpty()) {
            String details = getOutOfRangeDetails(farmList.get(0), activeProfile);
            if (!details.isEmpty()) sendOutOfRangeNotification(details, activeProfile);
        }
    }



    public void stopMonitoringService() {
        Intent serviceIntent = new Intent(requireContext(), FarmMonitoringService.class);
        requireContext().stopService(serviceIntent);
        Log.d("MainFragment", "🛑 Monitoring service stopped");
        Toast.makeText(getContext(), "Background monitoring stopped", Toast.LENGTH_SHORT).show();
    }

    private void startPeriodicRefresh() {
        if (refreshTimer != null) { refreshTimer.cancel(); refreshTimer = null; }
        refreshTimer = new Timer();
        refreshTask  = new TimerTask() {
            @Override public void run() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override public void run() { loadFarmData(); }
                    });
                }
            }
        };
        refreshTimer.schedule(refreshTask, 0, FarmTimer);
    }

    private void stopPeriodicRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
            refreshTask  = null;
        }
    }

    private void setupNetworkMonitoring() {
        connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Internet restored – dismiss dialog if showing, optionally notify user
                requireActivity().runOnUiThread(() -> {
                    if (noNetworkDialog != null && noNetworkDialog.isShowing()) {
                        noNetworkDialog.dismiss();
                        noNetworkDialog = null;
                    }
                    // Optional: brief toast to confirm reconnection
                    // Toast.makeText(getContext(), "Internet connection restored", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                // Internet lost – show dialog if not already showing
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return; // fragment detached

                    if (noNetworkDialog == null || !noNetworkDialog.isShowing()) {
                        noNetworkDialog = new AlertDialog.Builder(requireContext())
                                .setTitle("No Internet Connection")
                                .setMessage("Please check your Wi‑Fi or mobile data.")
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .setCancelable(true)      // allow user to dismiss
                                .show();
                    }
                });
            }
        };

        // Register the callback (works from API 21+)
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }
}

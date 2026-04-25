package com.example.smartfarmapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * FARM MONITORING SERVICE - COMPLETE BEGINNER'S GUIDE (HELP ME)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS A SERVICE? (By Claude Code)
 * ------------------
 * A Service is a special Android component that runs in the BACKGROUND, even when
 * the user is not looking at your app. Think of it like a worker that keeps doing
 * its job behind the scenes.
 *
 * WHY DO WE NEED THIS SERVICE?
 * -----------------------------
 * 1. AUTO-REFRESH: Automatically fetch new farm data every few minutes (like a timer for the loadFarmData)
 * 2. MONITORING: Check if sensor values are out of range (Vegetation out of range sensors)
 * 3. NOTIFICATIONS: Alert the user when something goes wrong (Alert if something goes wrong)
 * 4. BACKGROUND WORK: Keep working even if the user switches to another app (Background tasks)
 *
 * HOW DOES IT WORK?
 * -----------------
 * 1. The service starts when the user opens MainFragment
 * 2. It uses a Handler to schedule repeating tasks (like an alarm clock)
 * 3. Every X minutes, it fetches new farm data
 * 4. If values are out of range, it sends a notification
 * 5. It broadcasts the new data so the UI can update
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class FarmMonitoringService extends Service {

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION CONSTANTS - EASY TO CHANGE!
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * HOW OFTEN TO CHECK FOR NEW DATA (in milliseconds)
     *
     * Current setting: 2 minutes (120,000 milliseconds)
     *
     * TO CHANGE THE REFRESH INTERVAL, MODIFY THIS NUMBER:
     * - 1 minute  = 60,000
     * - 2 minutes = 120,000
     * - 5 minutes = 300,000
     * - 10 minutes = 600,000
     */
    private static final long REFRESH_INTERVAL_MS =120000; // 2 minutes

    /**
     * Notification channel IDs - these are required by Android for notifications
     */
    private static final String CHANNEL_ID_FOREGROUND = "FarmMonitoringService";
    public static final String CHANNEL_ID_ALERTS = "FarmAlerts";

    /**
     * Unique IDs for different types of notifications
     */
    private static final int NOTIFICATION_ID_FOREGROUND = 1001;
    // This is a System Requirement. It’s the notification that tells the user the service is currently running.
    // It is Ongoing. You cannot swipe it away.
    private static final int NOTIFICATION_ID_ALERT = 2001;
    // This is the Emergency Notification. It only pops up when something is actually wrong.

    /**
     * Action name for broadcasting updates to the UI
     */
    public static final String ACTION_DATA_UPDATED = "com.example.smartfarmapp.DATA_UPDATED";
    // ═══════════════════════════════════════════════════════════════════════
    // INSTANCE VARIABLES (The Service's Memory)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Handler - Think of this as a "task scheduler"
     * It allows us to run code repeatedly at specific intervals
     */
    private Handler monitoringHandler;

    /**
     * Runnable - Think of this as a "task" that can be run
     * This specific task will fetch and check farm data
     */
    private Runnable monitoringRunnable;

    /**
     * Repository for fetching farm data from the database
     */
    private SupabaseService supabaseService; // The Farm's data Repo

    /**
     * Repository for fetching vegetation profiles (the ideal ranges)
     */
    private VegetationRepo vegetationRepo;

    /**
     * The current active vegetation profile being monitored
     */
    private Vegetation activeVegetation;

    /**
     * Flag to track if we've already sent an alert (to avoid spam)
     */
    private boolean hasNotifiedOutOfRange = false;

    /**
     * The timestamp of the last data we checked (to avoid duplicate alerts)
     */
    private String lastCheckedDateTime = "";

    // ═══════════════════════════════════════════════════════════════════════
    // SERVICE LIFECYCLE METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * onCreate() - Called ONCE when the service is first created
     *
     * This is like the constructor - we set up everything we need here.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FarmMonitoringService", "📱 Service created!");

        // Initialize our data repositories
        supabaseService = new SupabaseService();
        vegetationRepo = new VegetationRepo();

        // Create the notification channels (required for Android 8.0+)
        createNotificationChannels();
    }

    /**
     * onStartCommand() - Called EVERY TIME someone starts the service
     *
     * This is where we receive commands and decide what to do.
     *
     * @param intent The intent used to start this service (contains extra info)
     * @param flags Additional data about this start request
     * @param startId A unique identifier for this specific start request
     * @return How the system should handle restarting this service if it gets killed
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("FarmMonitoringService", "🚀 Service starting...");

        // Load the active vegetation profile from SharedPreferences
        loadActiveVegetation();

        // Start as a FOREGROUND service
        // (Foreground services show a persistent notification and are less likely to be killed)
        startForeground(NOTIFICATION_ID_FOREGROUND, createForegroundNotification());

        // Start the monitoring loop
        startMonitoring();

        // START_STICKY means: if Android kills this service due to low memory,
        // it should automatically restart it when resources become available again
        return START_STICKY;
    }

    /**
     * onBind() - Required method for services, but we don't use binding here
     *
     * Binding is for when other components want to directly communicate with the service.
     * We use broadcasts instead, so we return null.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't support binding
    }

    /**
     * onDestroy() - Called when the service is being shut down
     *
     * Clean up resources and stop all background tasks.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("FarmMonitoringService", "🛑 Service destroyed!");

        // Stop the monitoring loop
        if (monitoringHandler != null && monitoringRunnable != null) {
            monitoringHandler.removeCallbacks(monitoringRunnable);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MONITORING LOGIC - THE HEART OF THE SERVICE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Starts the monitoring loop that repeatedly checks farm data
     *
     * HOW IT WORKS:
     * 1. Creates a Handler (task scheduler) on the main thread
     * 2. Creates a Runnable (the task to repeat)
     * 3. Schedules the task to run immediately
     * 4. The task reschedules itself to run again after REFRESH_INTERVAL_MS
     */
    private void startMonitoring() {
        // Create a Handler tied to the main thread's message queue
        monitoringHandler = new Handler(Looper.getMainLooper());

        // Create the task that will fetch and check data
        monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("FarmMonitoringService", "🔄 Fetching farm data...");

                // Fetch the latest farm data
                fetchAndCheckFarmData();

                // Schedule this task to run again after REFRESH_INTERVAL_MS
                monitoringHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };

        // Start the loop immediately
        monitoringHandler.post(monitoringRunnable);
    }

    /**
     * Fetches farm data and checks if values are out of range
     *
     * FLOW:
     * 1. Get user ID from SharedPreferences
     * 2. Call SupabaseService to fetch farm data
     * 3. If successful, check the latest reading
     * 4. If out of range, send notification
     * 5. Broadcast update to UI
     */
    private void fetchAndCheckFarmData() {
        // Get the logged-in user's ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            Log.e("FarmMonitoringService", "❌ No user ID found - cannot fetch data");
            return;
        }

        // Fetch farm data from the database

        supabaseService.fetchFarms(userId, new SupabaseService.FarmCallback() {
            @Override
            public void onSuccess(List<Farm> farms) {
                Log.d("FarmMonitoringService", "✅ Fetched " + farms.size() + " farm records");

                if (!farms.isEmpty()) {
                    Farm latestFarm = farms.get(0);

                    // Check if this is new data
                    if (!latestFarm.getDateTime().equals(lastCheckedDateTime)) {
                        lastCheckedDateTime = latestFarm.getDateTime();
                        hasNotifiedOutOfRange = false;
                    }

                    // Check for out-of-range values
                    checkAndNotifyIfOutOfRange(latestFarm);


                }


                // ✅ CRITICAL: Always broadcast, even if no alerts
                // This tells MainFragment to refresh the RecyclerView
                broadcastDataUpdate();
                Log.d("FarmMonitoringService", "📡 Broadcast sent to update UI");
            }

            @Override
            public void onFailure(Exception error) {
                Log.e("FarmMonitoringService", "❌ Failed to fetch data: " + error.getMessage());
            }
        });
    }

    /**
     * Checks if farm sensor values are out of the acceptable range
     * and sends a notification if they are
     *
     * @param farm The latest farm reading to check
     */
    private void checkAndNotifyIfOutOfRange(Farm farm) {
        // If no vegetation profile is set, we can't check ranges
        if (activeVegetation == null) {
            Log.d("FarmMonitoringService", "ℹ️ No active vegetation - skipping range check");
            return;
        }

        // If we already sent an alert for this reading, don't spam
        if (hasNotifiedOutOfRange) {
            return;
        }

        // Determine if it's daytime or nighttime (affects which ranges to use)
        boolean isDay = isDayTime(farm.getDateTime());

        // Build a detailed message about what's wrong
        StringBuilder alertMessage = new StringBuilder();
        boolean hasIssues = false;

        // --- CHECK TEMPERATURE ---
        float tempMin = isDay ? activeVegetation.getDayTempMin() : activeVegetation.getNightTempMin();
        // this is
        /**float tempMin;

        if (isDay == true) {
            tempMin = activeVegetation.getDayTempMin();
        } else {
            tempMin = activeVegetation.getNightTempMin();
        }*/
        float tempMax = isDay ? activeVegetation.getDayTempMax() : activeVegetation.getNightTempMax();

        if (farm.getTemp() < tempMin || farm.getTemp() > tempMax) {
            alertMessage.append("🌡️ Temperature: ").append(farm.getTemp())
                    .append("°C (Expected: ").append(tempMin).append("-").append(tempMax).append("°C)\n");
            hasIssues = true;
        }

        // --- CHECK GROUND HUMIDITY ---
        float groundMin = isDay ? activeVegetation.getDayGroundHumidMin() : activeVegetation.getNightGroundHumidMin();
        float groundMax = isDay ? activeVegetation.getDayGroundHumidMax() : activeVegetation.getNightGroundHumidMax();

        if (farm.getGroundHumid() < groundMin || farm.getGroundHumid() > groundMax) {
            alertMessage.append("💧 Ground Humidity: ").append(farm.getGroundHumid())
                    .append("% (Expected: ").append(groundMin).append("-").append(groundMax).append("%)\n");
            hasIssues = true;
        }

        // --- CHECK AIR HUMIDITY ---
        float airMin = isDay ? activeVegetation.getDayAirHumidMin() : activeVegetation.getNightAirHumidMin();
        float airMax = isDay ? activeVegetation.getDayAirHumidMax() : activeVegetation.getNightAirHumidMax();

        if (farm.getAirHumid() < airMin || farm.getAirHumid() > airMax) {
            alertMessage.append("💨 Air Humidity: ").append(farm.getAirHumid())
                    .append("% (Expected: ").append(airMin).append("-").append(airMax).append("%)");
            hasIssues = true;
        }

        // If any issues were found, send a notification
        if (hasIssues) {
            sendAlertNotification(
                    "⚠️ Farm Alert: Values Out of Range!",
                    alertMessage.toString(),
                    activeVegetation.getName()
            );
            hasNotifiedOutOfRange = true; // Don't send duplicate alerts
            Log.w("FarmMonitoringService", "⚠️ Out of range detected!");
        } else {
            Log.d("FarmMonitoringService", "✅ All values within range");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Determines if a timestamp represents daytime or nighttime
     *
     * Daytime is defined as 6:00 AM to 5:59 PM
     *
     * @param isoDate The timestamp in ISO format from the database
     * @return true if daytime, false if nighttime
     */
    private boolean isDayTime(String isoDate) {
        if (isoDate == null) return true;
        try {
            java.text.SimpleDateFormat parser = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    java.util.Locale.US
            );
            java.util.Date date = parser.parse(isoDate.substring(0, 19));
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            return hour >= 6 && hour < 18;
        } catch (Exception e) {
            return true; // Default to daytime on error
        }
    }

    /**
     * Loads the active vegetation profile from SharedPreferences
     *
     * The active profile is stored as a JSON string, which we convert back
     * to a Vegetation object using Gson.
     */
    private void loadActiveVegetation() {
        SharedPreferences prefs = getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("active_vegetation", null);

        if (json != null) {
            activeVegetation = new com.google.gson.Gson().fromJson(json, Vegetation.class);
            Log.d("FarmMonitoringService", "✅ Loaded active vegetation: " +
                    (activeVegetation != null ? activeVegetation.getName() : "null"));
        } else {
            Log.d("FarmMonitoringService", "ℹ️ No active vegetation set");
        }
    }

    /**
     * Sends a broadcast to notify the UI that new data is available
     *
     * MainFragment will listen for this broadcast and refresh the RecyclerView
     */
    private void broadcastDataUpdate() {
        Log.d("FarmMonitoringService", "Broadcasting: " + ACTION_DATA_UPDATED);
        Intent intent = new Intent(ACTION_DATA_UPDATED);
        sendBroadcast(intent);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NOTIFICATION SYSTEM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates notification channels (required for Android 8.0 and above)
     *
     * WHAT IS A NOTIFICATION CHANNEL?
     * Channels let users customize notification settings for different types
     * of notifications. For example, they can silence "Monitoring Active" but
     * keep "Alerts" at full volume.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Channel 1: Foreground service notification (low priority, no sound)
            NotificationChannel foregroundChannel = new NotificationChannel(
                    CHANNEL_ID_FOREGROUND,
                    "Farm Monitoring Active",
                    NotificationManager.IMPORTANCE_LOW // Low priority - not intrusive
            );
            foregroundChannel.setDescription("Shows when farm monitoring is running");
            foregroundChannel.setShowBadge(false);
            manager.createNotificationChannel(foregroundChannel);

            // Channel 2: Alert notifications (high priority, with sound)
            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "Farm Alerts",
                    NotificationManager.IMPORTANCE_HIGH // High priority - gets attention
            );
            alertChannel.setDescription("Alerts when sensor values are out of range");
            alertChannel.setShowBadge(true);
            alertChannel.enableVibration(true);
            manager.createNotificationChannel(alertChannel);
        }
    }

    /**
     * Creates the persistent notification shown while monitoring is active
     *
     * This notification is required for foreground services. It lets the user
     * know the service is running and prevents Android from killing it.
     *
     * @return A Notification object
     */
    private Notification createForegroundNotification() {
        // Create an intent to open the app when the notification is tapped
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        return new NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
                .setContentTitle("🌱 Farm Monitoring Active")
                .setContentText("Checking farm conditions every " + (REFRESH_INTERVAL_MS / 60000) + " minutes")
                .setSmallIcon(android.R.drawable.ic_menu_compass) // Use a system icon (you can replace this)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Can't be dismissed by swiping
                .setPriority(NotificationCompat.PRIORITY_LOW) // Low priority - not intrusive
                .build();
    }

    /**
     * Sends an alert notification when values are out of range
     *
     * @param title The notification title
     * @param message The detailed message about what's wrong
     * @param vegetationName The name of the vegetation being monitored
     */
    /**
     * Sends a push notification to the user when a sensor threshold is breached.
     * This method handles the creation of the notification, its visual style,
     * and the logic for reopening the app when the user taps it.
     */
    private void sendAlertNotification(String title, String message, String vegetationName) {

        // 1. Create a standard Intent to define the destination.
        // We want to open MainActivity when the notification is clicked.
        Intent intent = new Intent(this, MainActivity.class);

        // Flags to ensure that clicking the notification doesn't create multiple
        // instances of the app; instead, it brings the existing one to the front.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        /*
         * 2. Wrap the Intent in a PendingIntent.
         * A PendingIntent is a "token" we give to the Android System.
         * Since our Service might be destroyed by the time the user clicks the notification,
         * the System uses this token to execute the Intent on our behalf.
         * * FLAG_IMMUTABLE: Required for security in modern Android versions (API 31+).
         * FLAG_UPDATE_CURRENT: If a PendingIntent already exists, update its data.
         */
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        /*
         * 3. Build the notification using NotificationCompat.Builder.
         * Using 'Compat' ensures the notification looks and works correctly
         * across different Android versions.
         */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // The icon shown in the status bar
                .setContentTitle(title)                       // e.g., "High Temperature Alert!"
                .setContentText(message)                      // e.g., "Tomato plant is at 35°C"
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Enables "Heads-up" (pop-up) behavior
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Helps the OS prioritize the alert
                .setAutoCancel(true)                          // Removes notification after user clicks it
                .setContentIntent(pendingIntent);             // Attach the PendingIntent defined above

        /*
         * 4. NotificationManager: The system service that manages all notifications.
         */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            /*
             * By using a fixed NOTIFICATION_ID, any new alert will replace the old one.
             * This prevents the user's notification tray from being flooded with
             * dozens of similar alerts if the temperature stays out of range.
             */
            notificationManager.notify(NOTIFICATION_ID_ALERT, builder.build());
        }
    }
}
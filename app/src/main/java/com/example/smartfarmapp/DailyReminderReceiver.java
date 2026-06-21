package com.example.smartfarmapp;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * DAILY REMINDER RECEIVER - THE NOTIFICATION TRIGGER
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS A BROADCAST RECEIVER?
 * -----------------------------
 * Think of it like a "listener" or a "radio" that is tuned to specific frequencies.
 * It waits for "Broadcasts" (messages) from the Android System or other apps.
 * Even if your app is closed, Android can "wake up" this class to handle an event.
 *
 * WHY DO WE NEED THIS?
 * --------------------
 * 1. ALARMS: The AlarmManager sends a broadcast when it's 8:00 AM. This class catches it.
 * 2. REBOOTS: When the phone restarts, all scheduled alarms are DELETED by Android.
 *    We catch the "BOOT_COMPLETED" broadcast to reschedule our alarm so it keeps working.
 *
 * BACKGROUND FOR ABSOLUTE BEGINNERS:
 * - A "Broadcast" is like a shout from the system that everyone can hear.
 * - This class says "I'm listening for the 8:00 AM shout!"
 * - Even if the user swiped the app away and it's not "running", Android keeps a 
 *   small record that this class needs to be woken up when that specific shout happens.
 *
 * HOW IT FITS INTO THE ANDROID SYSTEM:
 * - Your app is not always running. To save battery, Android "kills" apps that aren't being used.
 * - However, some things need to happen even if the app is dead.
 * - The Android System (the OS) stays awake. We register this Receiver in the "AndroidManifest.xml" file.
 * - That registration is like giving the OS a phone number to call when a specific event happens.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class DailyReminderReceiver extends BroadcastReceiver {

    // A tag used for identifying log messages in the "Logcat" (the console)
    // This is like a "Name Tag" for our debug messages so we can find them in the crowd.
    // When you're debugging, you can filter Logcat by "DailyReminderReceiver" to see only these logs.
    private static final String TAG = "DailyReminderReceiver";

    // A unique number for this notification. If we send another one with the same ID,
    // it replaces the old one instead of creating a new one in the list.
    // This prevents the user's phone from being filled with 50 "Morning Check" messages.
    // Android uses this integer to keep track of active notifications for your app.
    private static final int NOTIFICATION_ID = 1001;

    /**
     * onReceive() - THE BRAIN OF THE RECEIVER.
     * This is the "Entry Point". When the OS "shouts" a message we care about, it executes this method.
     * WARNING: You only have about 10 seconds to finish your work here before Android kills the process.
     * Don't do heavy work like downloading files here!
     *
     * @param context The "Environment". Think of it as a bridge to the Android System. 
     *                It lets you access storage, start screens, or send notifications.
     * @param intent  The "Package". It contains the data about the event that happened.
     *                For example, it tells us if this was a "Boot" event or a "Timer" event.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        
        // 1. SAFETY CHECK: Check if the user actually wants reminders.
        // We use SimpleAlarmManager to check our "Saved Settings" (SharedPreferences).
        // Why? Because even if the alarm triggers, we should check if the user 
        // turned the switch "OFF" since the last time the alarm was set.
        // This prevents "Zombie Notifications" that keep coming even after the user tried to stop them.
        if (!SimpleAlarmManager.isDailyReminderEnabled(context)) {
            Log.d(TAG, "Reminder triggered but it's disabled in settings. Cancelling future alarms.");
            // If it's disabled, we stop any further scheduled tasks to save battery.
            // This cleans up the system so it stops trying to wake us up.
            SimpleAlarmManager.cancelDailyAlarm(context);
            return; // EXIT: "return" stops the method immediately. We don't show the notification.
        }

        // 2. CHECK THE REASON: Why was this called?
        // ACTION_BOOT_COMPLETED is a special signal sent by Android when the phone finishes starting up.
        // We need to know this because Android's alarm clock is "volatile" (it wipes clean on power off).
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // WHY? Android clears all alarms on reboot for security/stability. 
            // If we don't catch this signal and re-set the alarm, the user will never 
            // get a reminder again until they manually open the app!
            // We call our helper to set the 8:00 AM timer again.
            SimpleAlarmManager.scheduleDailyAlarm(context);
            Log.d(TAG, "Alarm re-scheduled after boot");
        } 
        // Otherwise, it was triggered by the AlarmManager (meaning it's 8:00 AM right now).
        else {
            Log.d(TAG, "Daily alarm triggered! Showing notification...");
            
            // Show the actual visual message to the user.
            // We pass "context" so the method can talk to the System Notification service.
            showNotification(context);
            
            // IMPORTANT: Modern Android "Exact Alarms" are "One-Shot". 
            // Once they fire, they are GONE from the system's memory.
            // They don't repeat automatically like a snooze button.
            // To make it happen again tomorrow at 8:00 AM, we must "re-arm" the alarm clock now.
            SimpleAlarmManager.scheduleDailyAlarm(context);
        }
    }

    /**
     * showNotification() - The logic that builds and displays the alert.
     * This method translates code into a physical "pop-up" on the user's screen.
     *
     * @param context Needed to access the NotificationManager system service.
     */
    private void showNotification(Context context) {
        
        // STEP 1: Define what happens when the user clicks the notification.
        // We want to open the "MainActivity" (the main screen of our app).
        // An "Intent" is basically a "Plan" or an "Order" to open a specific screen.
        Intent mainIntent = new Intent(context, MainActivity.class);
        
        // STEP 2: Wrap the Intent in a "PendingIntent".
        // BACKGROUND: A PendingIntent is like a "Permission Slip" or a "Coupon". 
        // Because our app might be CLOSED when the notification is clicked, 
        // we can't just start the screen ourselves. 
        // Instead, we give this slip to the Android System (The mayor of the phone).
        // It says: "Hey System, I'm giving you permission to open my MainActivity on my behalf 
        // whenever the user taps this notification, even if I'm not running."
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, // Unique request code (not used here, but good if you had multiple different alerts)
                mainIntent, 
                // FLAG_IMMUTABLE: This is a security requirement for Android 12+.
                // It means the System or other malicious apps cannot change the 'Plan' inside the slip.
                // It's like putting the permission slip in a sealed, tamper-proof envelope.
                PendingIntent.FLAG_IMMUTABLE 
        );

        // STEP 3: Build the visual notification.
        // We use NotificationCompat.Builder because it handles different Android versions automatically.
        // It's like a "Lego Set" where we add pieces (icon, title, text) one by one.
        // We connect it to "FarmMonitoringService.CHANNEL_ID_ALERTS" so it follows the same "High Importance" rules.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FarmMonitoringService.CHANNEL_ID_ALERTS)
                // The icon that appears in the top status bar (usually a small white silhouette).
                // Without this, the notification won't even show up on many Android versions.
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                // The big bold text at the top of the notification.
                .setContentTitle("🌱 Morning Farm Check")
                // The smaller descriptive text below the title.
                .setContentText("Time to check your crop conditions!")
                // PRIORITY_HIGH makes it "pop up" or "peek" at the top of the screen (Heads-up).
                // Without this, it might just hide quietly in the notification tray without a sound.
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Automatically removes the notification from the list when the user taps it.
                // If set to false, it stays there forever until the user swipes it away manually.
                .setAutoCancel(true)
                // Attach the "permission slip" (PendingIntent) we created in Step 2.
                .setContentIntent(pendingIntent);

        // STEP 4: Send the notification to the Android System.
        // NotificationManager is the system's "Post Office". 
        // We "check out" this service from the System using getSystemService.
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            // Deliver the notification. 
            // notify() takes the ID and the finished "Package" (builder.build()).
            // The NOTIFICATION_ID (1001) ensures that if we send this again tomorrow, 
            // it replaces the old one instead of making a long list of identical reminders.
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}

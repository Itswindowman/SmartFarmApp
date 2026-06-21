package com.example.smartfarmapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SIMPLE ALARM MANAGER - THE SCHEDULER
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS ALARM MANAGER?
 * ----------------------
 * AlarmManager is a system service that allows you to schedule your app to run
 * at a specific time in the future, even if your app is currently closed.
 * It's like setting a physical alarm clock that wakes up the app.
 *
 * WHY DO WE NEED THIS?
 * --------------------
 * We want to remind the user every day at 8:00 AM to check their farm.
 * Since we can't expect the user to keep the app open 24/7, we tell Android:
 * "Hey, at 8:00 AM, please send a message (Broadcast) to our app."
 *
 * KEY CONCEPTS:
 * 1. PENDING INTENT: A "token" or "permission slip" given to the system to 
 *    execute an action on our behalf later.
 * 2. RTC_WAKEUP: A type of alarm that uses the "Real Time Clock" (actual time)
 *    and WAKES UP the device if the screen is off.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class SimpleAlarmManager {

    private static final String TAG = "SimpleAlarmManager";
    
    // Names for saving settings so they persist after the app closes
    private static final String PREFS_NAME = "SmartFarmAlarmPrefs";
    private static final String KEY_ALARM_ENABLED = "daily_reminder_enabled";

    /**
     * setDailyReminder() - Turns the daily alarm ON or OFF.
     * 
     * @param context Needed to access system services and shared preferences.
     * @param enable  True to start the alarm, False to stop it.
     */
    public static void setDailyReminder(Context context, boolean enable) {
        // SharedPreferences: A small "database" for saving simple settings (like a toggle).
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (enable) {
            // STEP 1: Actually tell the Android System to schedule the alarm.
            boolean success = scheduleDailyAlarm(context);
            if (success) {
                // STEP 2: Save the state so the Switch button stays "ON" next time we open the app.
                editor.putBoolean(KEY_ALARM_ENABLED, true);
                editor.apply();
                Toast.makeText(context, "Daily reminder enabled! (8:00 AM daily)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to enable reminder", Toast.LENGTH_SHORT).show();
            }
        } else {
            // STEP 1: Tell Android to stop sending the 8:00 AM messages.
            cancelDailyAlarm(context);
            // STEP 2: Save the state as "OFF".
            editor.putBoolean(KEY_ALARM_ENABLED, false);
            editor.apply();
            Toast.makeText(context, "Daily reminder disabled", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * isDailyReminderEnabled() - Checks if the user previously turned the alarm on.
     */
    public static boolean isDailyReminderEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Default to 'false' if we've never saved anything yet.
        return prefs.getBoolean(KEY_ALARM_ENABLED, false);
    }

    /**
     * scheduleDailyAlarm() - The technical logic to talk to Android's Alarm system.
     */
    public static boolean scheduleDailyAlarm(Context context) {
        try {
            // Get the AlarmManager service from the Android System.
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return false;

            // Define WHERE the message should go. 
            // We want it to go to 'DailyReminderReceiver'.
            Intent intent = new Intent(context, DailyReminderReceiver.class);
            
            // Wrap the intent in a PendingIntent. 
            // This is required because AlarmManager runs OUTSIDE our app process.
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    0, 
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // STEP 1: Set the clock to 8:00 AM.
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 8); 
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // STEP 2: Logic check. If it's already 9:00 AM today, 
            // we should set the alarm for 8:00 AM TOMORROW instead of triggering it immediately.
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // STEP 3: Tell Android to set the alarm.
            // setExactAndAllowWhileIdle: This is the "Strongest" type of alarm. 
            // It triggers even if the phone is in "Doze Mode" (deep sleep for battery saving).
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, // Use actual time & wake up the phone
                    calendar.getTimeInMillis(), // The target time in milliseconds
                    pendingIntent // Our "permission slip" to trigger the receiver
            );

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm", e);
            return false;
        }
    }

    /**
     * cancelDailyAlarm() - Tells Android to stop the scheduled alarm.
     */
    public static void cancelDailyAlarm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            // We must create an EXACT MATCH of the PendingIntent we used to schedule it.
            Intent intent = new Intent(context, DailyReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    0, 
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Tell the manager to delete any matching alarm.
            alarmManager.cancel(pendingIntent);
            // Also cancel the pending intent itself to free up memory.
            pendingIntent.cancel();
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling alarm", e);
        }
    }
}

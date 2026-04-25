package com.example.smartfarmapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

public class SimpleAlarmManager {

    private static final String TAG = "SimpleAlarmManager";
    private static final String PREFS_NAME = "SmartFarmAlarmPrefs";
    private static final String KEY_ALARM_ENABLED = "daily_reminder_enabled";

    /**
     * Enable or disable daily reminder
     */
    public static void setDailyReminder(Context context, boolean enable) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (enable) {
            boolean success = scheduleDailyAlarm(context);
            if (success) {
                editor.putBoolean(KEY_ALARM_ENABLED, true);
                editor.apply();
                Toast.makeText(context, "Daily reminder enabled! (8:00 AM daily)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to enable reminder", Toast.LENGTH_SHORT).show();
            }
        } else {
            cancelDailyAlarm(context);
            editor.putBoolean(KEY_ALARM_ENABLED, false);
            editor.apply();
            Toast.makeText(context, "Daily reminder disabled", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if daily reminder is enabled
     */
    public static boolean isDailyReminderEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ALARM_ENABLED, false);
    }

    /**
     * Schedule the daily alarm for 8:00 AM
     */
    public static boolean scheduleDailyAlarm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return false;

            Intent intent = new Intent(context, DailyReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Calculate time for 8:00 AM
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 8); // 8
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // If it's already past 8:00 AM, set for tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Use setExactAndAllowWhileIdle for modern Android
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm", e);
            return false;
        }
    }

    /**
     * Cancel the daily alarm
     */
    public static void cancelDailyAlarm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, DailyReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling alarm", e);
        }
    }
}
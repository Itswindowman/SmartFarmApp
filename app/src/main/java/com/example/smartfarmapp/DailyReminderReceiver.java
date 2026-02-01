package com.example.smartfarmapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class DailyReminderReceiver {

    private static final String PREFS_NAME = "SmartFarmAlarmPrefs";
    private static final String KEY_ALARM_ENABLED = "daily_reminder_enabled";

    /**
     * Enable or disable daily reminder
     */
    public static void setDailyReminder(Context context, boolean enable) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (enable) {
            // Set alarm for 7 PM daily
            boolean success = scheduleDailyAlarm(context);
            if (success) {
                editor.putBoolean(KEY_ALARM_ENABLED, true);
                editor.apply();
                Toast.makeText(context, "Daily reminder enabled! (7 PM daily)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to enable reminder", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Cancel alarm
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
     * Schedule the daily alarm for 7 PM
     */
    private static boolean scheduleDailyAlarm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return false;

            Intent intent = new Intent(context, DailyReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Calculate time for 7 PM
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 19); // 7 PM
            calendar.set(java.util.Calendar.MINUTE, 0);
            calendar.set(java.util.Calendar.SECOND, 0);

            // If it's already past 7 PM, set for tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1);
            }

            // Set inexact repeating alarm (no permission needed)
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Cancel the daily alarm
     */
    private static void cancelDailyAlarm(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, DailyReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();

        } catch (Exception e) {
            // Ignore errors
        }
    }
}
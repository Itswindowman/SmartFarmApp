package com.example.smartfarmapp;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class DailyReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "DailyReminderReceiver";
    private static final int NOTIFICATION_ID = 1001;

    /**
     * Precondition: context and intent are provided by the system when the broadcast is received
     * Postcondition: Shows a notification if the reminder is enabled, and reschedules the alarm for the next day or after boot
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Only proceed if the daily reminder is actually enabled in SharedPreferences
        if (!SimpleAlarmManager.isDailyReminderEnabled(context)) {
            Log.d(TAG, "Reminder triggered but it's disabled in settings. Cancelling future alarms.");
            SimpleAlarmManager.cancelDailyAlarm(context);
            return;
        }

        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Re-schedule alarm on boot because it's enabled
            SimpleAlarmManager.scheduleDailyAlarm(context);
            Log.d(TAG, "Alarm re-scheduled after boot");
        } else {
            // This is the actual alarm trigger - show notification
            showNotification(context);
            
            // Re-schedule for the next day (since we use setExactAndAllowWhileIdle which is a one-shot)
            SimpleAlarmManager.scheduleDailyAlarm(context);
        }
    }

    /**
     * Precondition: context is not null
     * Postcondition: Displays a "Morning Farm Check" notification that opens MainActivity when clicked
     */
    private void showNotification(Context context) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FarmMonitoringService.CHANNEL_ID_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🌱 Morning Farm Check")
                .setContentText("Time to check your crop conditions!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
package com.example.foodexpirytracker.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.foodexpirytracker.MainActivity;
import com.example.foodexpirytracker.R;

/*
 * Function: NotificationHelper
 * Purpose: Create notification channel and send app notifications
 */
public class NotificationHelper {
    public static final String CHANNEL_ID = "expiry_alerts";
    public static final String CHANNEL_NAME = "Expiry Alerts";
    public static final String CHANNEL_DESC = "Reminders about food expiry dates";

    /*
     * Function: createChannel
     * Purpose: Register notification channel on Android O and above
     */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /*
     * Function: sendNotification (explicit id)
     * Purpose: Build and dispatch a notification to open MainActivity
     */
    public static void sendNotification(Context context, String title, String message, int notificationId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    /*
     * Function: sendNotification
     * Purpose: Convenience overload using a time-based notification id
     */
    public static void sendNotification(Context context, String title, String message) {
        sendNotification(context, title, message, (int) System.currentTimeMillis());
    }
}
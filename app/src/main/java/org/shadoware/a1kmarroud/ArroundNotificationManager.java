package org.shadoware.a1kmarroud;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

class ArroundNotificationManager {
    private static final String CHANNEL_DEFAULT_IMPORTANCE = "Distance";
    public static final int ARROUND_ID = 1;
    private final Context context;

    ArroundNotificationManager(Context context) {
        this.context = context;
        this.createNotificationChannel();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            NotificationChannel channel = new NotificationChannel(CHANNEL_DEFAULT_IMPORTANCE, name, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(description);
            channel.setSound(null, null);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public Notification createNotification(float distance) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(context, CHANNEL_DEFAULT_IMPORTANCE)
                .setContentTitle(context.getText(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_message, (int) distance))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setSound(null)
                .build();
    }

    public void notifyDistance(float distance) {
        Notification n = this.createNotification(distance);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.notify(ARROUND_ID, n);
    }
}

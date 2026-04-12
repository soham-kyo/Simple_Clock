package com.example.clock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RingtoneService extends Service {
    private static final String CHANNEL_ID = "RINGING_CHANNEL";
    private Ringtone ringtone;
    private final Handler stopHandler = new Handler();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if ("STOP".equals(action)) {
            stopRinging();
            return START_NOT_STICKY;
        }

        String uriString = intent.getStringExtra("RINGTONE_URI");
        int durationMinutes = intent.getIntExtra("DURATION_MINUTES", 1);
        String title = intent.getStringExtra("TITLE");

        if (title == null) title = "Clock";

        createNotificationChannel();

        Intent stopIntent = new Intent(this, RingtoneService.class);
        stopIntent.setAction("STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent fullScreenIntent = new Intent(this, MainActivity.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText("Tap to stop or open app")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, notification);
        }

        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
            }
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                AudioAttributes aa = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                ringtone.setAudioAttributes(aa);
                ringtone.play();
            }
        }

        // Auto-stop after duration
        stopHandler.removeCallbacksAndMessages(null);
        stopHandler.postDelayed(this::stopRinging, durationMinutes * 60 * 1000L);

        return START_NOT_STICKY;
    }

    private void stopRinging() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        stopForeground(true);
        stopSelf();

        // Notify MainActivity to hide stop buttons
        Intent intent = new Intent("ALARM_STOPPED");
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Ringing Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        serviceChannel.setSound(null, null); // Sound is handled by Ringtone object
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
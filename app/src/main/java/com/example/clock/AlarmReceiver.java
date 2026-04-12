package com.example.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String uriString = intent.getStringExtra("RINGTONE_URI");
        int durationMinutes = intent.getIntExtra("DURATION_MINUTES", 1);

        Intent serviceIntent = new Intent(context, RingtoneService.class);
        serviceIntent.putExtra("RINGTONE_URI", uriString);
        serviceIntent.putExtra("DURATION_MINUTES", durationMinutes);
        serviceIntent.putExtra("TITLE", "Alarm");

        context.startForegroundService(serviceIntent);
    }
}
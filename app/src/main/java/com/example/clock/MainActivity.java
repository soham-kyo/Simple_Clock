package com.example.clock;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "ClockSettings";
    private static final String KEY_ALARM_DURATION = "alarm_duration";
    private static final String KEY_TIMER_DURATION = "timer_duration";
    private static final String KEY_IS_DARK_MODE = "is_dark_mode";

    // Layouts
    private ScrollView clockLayout, stopwatchLayout, timerLayout;

    // Clock/Alarm
    private TextView alarmStatus;
    private Button btnDeleteAlarm;
    private Button btnStopAlarm;
    private Uri selectedRingtoneUri;
    private final Calendar alarmCalendar = Calendar.getInstance();

    // Stopwatch
    private TextView stopwatchDisplay, lapsDisplay;
    private Button btnStartStopwatch;
    private final Handler stopwatchHandler = new Handler();
    private long startTime = 0L, timeInMilliseconds = 0L, timeSwapBuff = 0L, updatedTime = 0L;
    private boolean isStopwatchRunning = false;
    private final List<String> lapsList = new ArrayList<>();

    // Timer
    private TextView timerDisplay;
    private Button btnStartTimer;
    private Button btnStopTimer;
    private final Handler timerHandler = new Handler();
    private long timerStartTime = 0L;
    private boolean isTimerRunning = false;
    private Uri selectedTimerRingtoneUri;

    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            btnStopAlarm.setVisibility(View.GONE);
            btnStopTimer.setVisibility(View.GONE);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference before super.onCreate
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_IS_DARK_MODE, true);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Ensure bars remain visible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        // Initialize tones directly from resources
        selectedRingtoneUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm_tone);
        selectedTimerRingtoneUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.timer_tone);

        // Initialize Layouts
        clockLayout = findViewById(R.id.clockLayout);
        stopwatchLayout = findViewById(R.id.stopwatchLayout);
        timerLayout = findViewById(R.id.timerLayout);

        ImageButton btnThemeToggle = findViewById(R.id.btnThemeToggle);
        btnThemeToggle.setImageResource(isDarkMode ? android.R.drawable.ic_menu_day : android.R.drawable.ic_menu_recent_history);
        btnThemeToggle.setOnClickListener(v -> toggleTheme());

        // Initialize Bottom Navigation
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_clock) {
                showLayout(clockLayout);
            } else if (id == R.id.nav_stopwatch) {
                showLayout(stopwatchLayout);
            } else if (id == R.id.nav_timer) {
                showLayout(timerLayout);
            }
            return true;
        });

        setupClockAlarm();
        setupStopwatch();
        setupTimer();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, stopReceiver, new IntentFilter("ALARM_STOPPED"), ContextCompat.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopReceiver, new IntentFilter("ALARM_STOPPED"));
        }
    }

    private void toggleTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_IS_DARK_MODE, true);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_DARK_MODE, !isDarkMode);
        editor.apply();

        if (!isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        recreate();
    }

    private void showLayout(ScrollView layoutToShow) {
        clockLayout.setVisibility(View.GONE);
        stopwatchLayout.setVisibility(View.GONE);
        timerLayout.setVisibility(View.GONE);
        layoutToShow.setVisibility(View.VISIBLE);
    }

    // --- Clock & Alarm Logic ---
    private void setupClockAlarm() {
        alarmStatus = findViewById(R.id.alarmStatus);
        Button btnSetAlarm = findViewById(R.id.btnSetAlarm);
        btnDeleteAlarm = findViewById(R.id.btnDeleteAlarm);
        btnStopAlarm = findViewById(R.id.btnStopAlarm);
        ImageButton btnAlarmSettings = findViewById(R.id.btnAlarmSettings);

        btnSetAlarm.setOnClickListener(v -> {
            if (checkNotificationPermission()) {
                if (checkExactAlarmPermission()) {
                    showAlarmPickerDialog();
                } else {
                    requestExactAlarmPermission();
                }
            } else {
                requestNotificationPermission();
            }
        });

        btnDeleteAlarm.setOnClickListener(v -> deleteAlarm());

        btnStopAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(this, RingtoneService.class);
            intent.setAction("STOP");
            startService(intent);
            btnStopAlarm.setVisibility(View.GONE);
        });

        btnAlarmSettings.setOnClickListener(v -> showDurationPickerDialog(KEY_ALARM_DURATION, "Alarm Ringing Duration"));
    }

    private void showAlarmPickerDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_alarm_picker);

        TimePicker timePicker = dialog.findViewById(R.id.timePicker);
        Button btnShowDatePicker = dialog.findViewById(R.id.btnShowDatePicker);
        TextView tvSelectedDate = dialog.findViewById(R.id.tvSelectedDate);
        CheckBox cbRepeat = dialog.findViewById(R.id.cbRepeat);
        Button btnConfirmAlarm = dialog.findViewById(R.id.btnConfirmAlarm);

        final Calendar selectedDate = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());

        btnShowDatePicker.setOnClickListener(v -> {
            @SuppressLint("SetTextI18n") DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        tvSelectedDate.setText("Date: " + dateFormat.format(selectedDate.getTime()));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        btnConfirmAlarm.setOnClickListener(v -> {
            alarmCalendar.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
            alarmCalendar.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
            alarmCalendar.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
            alarmCalendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            alarmCalendar.set(Calendar.MINUTE, timePicker.getMinute());
            alarmCalendar.set(Calendar.SECOND, 0);

            if (alarmCalendar.before(Calendar.getInstance())) {
                if (!cbRepeat.isChecked()) {
                    Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    alarmCalendar.add(Calendar.DATE, 1);
                }
            }

            setAlarm(cbRepeat.isChecked());
            dialog.dismiss();
        });

        dialog.show();
    }

    @SuppressLint("SetTextI18n")
    private void deleteAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        alarmStatus.setText("No alarm set");
        btnDeleteAlarm.setVisibility(View.GONE);
        Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show();
    }

    private boolean checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
            Toast.makeText(this, "Please allow exact alarms for this app to work correctly.", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void setAlarm(boolean repeat) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int durationMinutes = prefs.getInt(KEY_ALARM_DURATION, 1);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("RINGTONE_URI", selectedRingtoneUri.toString());
        intent.putExtra("DURATION_MINUTES", durationMinutes);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);

        if (alarmManager != null) {
            try {
                if (repeat) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmCalendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCalendar.getTimeInMillis(), pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmCalendar.getTimeInMillis(), pendingIntent);
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCalendar.getTimeInMillis(), pendingIntent);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, HH:mm", Locale.getDefault());
        alarmStatus.setText("Alarm: " + sdf.format(alarmCalendar.getTime()) + (repeat ? " (Daily)" : ""));
        btnDeleteAlarm.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Alarm set!", Toast.LENGTH_SHORT).show();
    }

    // --- Stopwatch Logic ---
    @SuppressLint("SetTextI18n")
    private void setupStopwatch() {
        stopwatchDisplay = findViewById(R.id.stopwatchDisplay);
        lapsDisplay = findViewById(R.id.lapsDisplay);
        btnStartStopwatch = findViewById(R.id.btnStartStopwatch);
        Button btnResetStopwatch = findViewById(R.id.btnResetStopwatch);
        Button btnLapStopwatch = findViewById(R.id.btnLapStopwatch);

        btnStartStopwatch.setOnClickListener(v -> {
            if (!isStopwatchRunning) {
                startTime = SystemClock.uptimeMillis();
                stopwatchHandler.postDelayed(updateStopwatchThread, 0);
                btnStartStopwatch.setText("Stop");
                isStopwatchRunning = true;
            } else {
                timeSwapBuff += timeInMilliseconds;
                stopwatchHandler.removeCallbacks(updateStopwatchThread);
                btnStartStopwatch.setText("Start");
                isStopwatchRunning = false;
            }
        });

        btnLapStopwatch.setOnClickListener(v -> {
            if (isStopwatchRunning) {
                String lapTime = stopwatchDisplay.getText().toString();
                lapsList.add(0, "Lap " + (lapsList.size() + 1) + ": " + lapTime);
                updateLapsDisplay();
            }
        });

        btnResetStopwatch.setOnClickListener(v -> {
            startTime = 0L;
            timeInMilliseconds = 0L;
            timeSwapBuff = 0L;
            updatedTime = 0L;
            stopwatchDisplay.setText("00:00:00.0");
            lapsList.clear();
            lapsDisplay.setText("");
            if (isStopwatchRunning) {
                stopwatchHandler.removeCallbacks(updateStopwatchThread);
                btnStartStopwatch.setText("Start");
                isStopwatchRunning = false;
            }
        });
    }

    private void updateLapsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (String lap : lapsList) {
            sb.append(lap).append("\n");
        }
        lapsDisplay.setText(sb.toString());
    }

    private final Runnable updateStopwatchThread = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            int hours = mins / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000) / 100;
            stopwatchDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d.%d", hours, mins % 60, secs, milliseconds));
            stopwatchHandler.postDelayed(this, 0);
        }
    };

    // --- Timer Logic ---
    @SuppressLint("SetTextI18n")
    private void setupTimer() {
        timerDisplay = findViewById(R.id.timerDisplay);
        Button btnSetTimer = findViewById(R.id.btnSetTimer);
        btnStartTimer = findViewById(R.id.btnStartTimer);
        Button btnResetTimer = findViewById(R.id.btnResetTimer);
        btnStopTimer = findViewById(R.id.btnStopTimer);
        ImageButton btnTimerSettings = findViewById(R.id.btnTimerSettings);

        btnSetTimer.setOnClickListener(v -> showTimerPickerDialog());

        btnStartTimer.setOnClickListener(v -> {
            if (!isTimerRunning && timerStartTime > 0) {
                isTimerRunning = true;
                btnStartTimer.setText("Pause");
                timerHandler.postDelayed(updateTimerThread, 1000);
            } else {
                isTimerRunning = false;
                btnStartTimer.setText("Start");
                timerHandler.removeCallbacks(updateTimerThread);
            }
        });

        btnResetTimer.setOnClickListener(v -> {
            isTimerRunning = false;
            btnStartTimer.setText("Start");
            timerHandler.removeCallbacks(updateTimerThread);
            Intent intent = new Intent(this, RingtoneService.class);
            intent.setAction("STOP");
            startService(intent);
            timerStartTime = 0;
            timerDisplay.setText("00:00:00");
            btnStopTimer.setVisibility(View.GONE);
        });

        btnStopTimer.setOnClickListener(v -> {
            Intent intent = new Intent(this, RingtoneService.class);
            intent.setAction("STOP");
            startService(intent);
            btnStopTimer.setVisibility(View.GONE);
        });

        btnTimerSettings.setOnClickListener(v -> showDurationPickerDialog(KEY_TIMER_DURATION, "Timer Ringing Duration"));
    }

    private void showTimerPickerDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_timer_picker);
        dialog.setTitle("Set Timer");

        final NumberPicker npHours = dialog.findViewById(R.id.npHours);
        final NumberPicker npMins = dialog.findViewById(R.id.npMins);
        final NumberPicker npSecs = dialog.findViewById(R.id.npSecs);

        npHours.setMinValue(0);
        npHours.setMaxValue(23);
        npMins.setMinValue(0);
        npMins.setMaxValue(59);
        npSecs.setMinValue(0);
        npSecs.setMaxValue(59);

        Button btnSet = dialog.findViewById(R.id.btnSet);
        btnSet.setOnClickListener(v -> {
            int hours = npHours.getValue();
            int mins = npMins.getValue();
            int secs = npSecs.getValue();
            timerStartTime = (hours * 3600L + mins * 60L + secs) * 1000L;
            updateTimerDisplay(timerStartTime);
            dialog.dismiss();
        });

        dialog.show();
    }

    private final Runnable updateTimerThread = new Runnable() {
        @SuppressLint("SetTextI18n")
        public void run() {
            if (timerStartTime > 0) {
                timerStartTime -= 1000;
                updateTimerDisplay(timerStartTime);
                timerHandler.postDelayed(this, 1000);
            } else {
                isTimerRunning = false;
                btnStartTimer.setText("Start");

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                int durationMinutes = prefs.getInt(KEY_TIMER_DURATION, 1);

                Intent serviceIntent = new Intent(MainActivity.this, RingtoneService.class);
                serviceIntent.putExtra("RINGTONE_URI", selectedTimerRingtoneUri.toString());
                serviceIntent.putExtra("DURATION_MINUTES", durationMinutes);
                serviceIntent.putExtra("TITLE", "Timer");

                startForegroundService(serviceIntent);

                btnStopTimer.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Timer Finished!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void updateTimerDisplay(long millis) {
        int secs = (int) (millis / 1000);
        int mins = secs / 60;
        int hours = mins / 60;
        secs = secs % 60;
        timerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins % 60, secs));
    }

    private void showDurationPickerDialog(String key, String title) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_duration_picker);

        TextView tvTitle = dialog.findViewById(R.id.tvDurationTitle);
        tvTitle.setText(title);

        NumberPicker npDuration = dialog.findViewById(R.id.npDuration);
        npDuration.setMinValue(1);
        npDuration.setMaxValue(60);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        npDuration.setValue(prefs.getInt(key, 1));

        Button btnSave = dialog.findViewById(R.id.btnSaveDuration);
        btnSave.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(key, npDuration.getValue());
            editor.apply();
            Toast.makeText(this, "Duration saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // --- Utility Methods ---
    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Ensure bars remain visible when focus changes
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(stopReceiver);
        } catch (Exception e) {
            // Receiver might not be registered
        }
    }
}
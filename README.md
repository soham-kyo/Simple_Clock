# 🕐 Simple Clock App — Android

A clean, feature-rich Android clock application with Alarm, Stopwatch, and Timer — all in one place. Built with Java and Material Design components.

---

## ✨ Features

### ⏰ Alarm
- Set a one-time alarm using a time picker
- Choose any date via a date picker dialog
- Customizable alarm ringing duration (1–60 minutes)
- Plays a built-in alarm tone as a foreground service
- Full-screen notification with a "Stop" action
- Alarm persists even when the app is closed

### ⏱️ Stopwatch
- Start, pause, and reset
- Lap recording with a scrollable lap list
- Displays hours, minutes, seconds, and milliseconds

### ⏲️ Timer
- Set hours, minutes, and seconds via a number picker
- Start, pause, and reset
- Plays a built-in timer tone when it reaches zero
- Customizable ringing duration (1–60 minutes)

### 🎨 UI
- Dark mode / Light mode toggle (persists across sessions)
- Bottom navigation bar (Clock / Stopwatch / Timer)
- Keeps screen on while the app is open

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 36 |
| UI | XML Views + Material Design |
| Navigation | BottomNavigationView |
| Background | Foreground Service (RingtoneService) |
| Scheduling | AlarmManager (exact alarms) |
| Persistence | SharedPreferences |

---

## 📂 Project Structure

```
app/
├── src/main/
│   ├── java/com/example/clock/
│   │   ├── MainActivity.java        # Main UI — clock, stopwatch, timer logic
│   │   ├── AlarmReceiver.java       # BroadcastReceiver — fires when alarm triggers
│   │   └── RingtoneService.java     # ForegroundService — plays alarm/timer sound
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml         # Main screen layout
│   │   │   ├── dialog_timer_picker.xml   # Timer set dialog
│   │   │   └── dialog_duration_picker.xml # Ringing duration dialog
│   │   ├── menu/
│   │   │   └── bottom_nav_menu.xml  # Bottom nav items
│   │   ├── raw/
│   │   │   ├── alarm_tone.mp3       # Alarm ringtone (add your own)
│   │   │   └── timer_tone.mp3       # Timer ringtone (add your own)
│   │   └── values/
│   │       ├── strings.xml
│   │       ├── colors.xml
│   │       └── themes.xml
│   └── AndroidManifest.xml
└── build.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (latest stable version)
- Android SDK API 26+
- Java 11 or higher

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/soham-kyo/simple_clock.git
   cd simple_clock
   ```

2. **Add sound files**

   The app requires two audio files in `app/src/main/res/raw/`:
    - `alarm_tone.mp3`
    - `timer_tone.mp3`

   > These are not included in the repo (to keep it lightweight). You can download free sounds from [freesound.org](https://freesound.org) and rename them accordingly.

3. **Open in Android Studio**

   Open Android Studio → File → Open → select the cloned folder.

4. **Sync Gradle**

   Click **"Sync Now"** when prompted, or go to File → Sync Project with Gradle Files.

5. **Run the app**

   Select a device (emulator or real phone) and click ▶ Run.

---

## 🔐 Permissions Used

| Permission | Reason |
|---|---|
| `POST_NOTIFICATIONS` | Show alarm/timer notifications (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Fire alarms at the exact scheduled time |
| `FOREGROUND_SERVICE` | Keep ringtone playing when app is in background |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Required for media-type foreground services |
| `WAKE_LOCK` | Wake the device screen when alarm fires |
| `USE_FULL_SCREEN_INTENT` | Show full-screen alarm notification |

---

## ⚙️ How It Works

```
User sets alarm
      ↓
AlarmManager schedules exact intent
      ↓
AlarmReceiver.onReceive() fires at alarm time
      ↓
Starts RingtoneService as ForegroundService
      ↓
Notification shown + ringtone plays
      ↓
Auto-stops after configured duration
   (or user taps "Stop")
```

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you'd like to change.

1. Fork the repo
2. Create your feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Soham Patil**
- GitHub: [@soham-kyo](https://github.com/soham-kyo)

---

_Built with Soham Patil️ using Android Studio & Java_

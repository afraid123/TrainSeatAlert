# Train Seat Alert — Android App

A fully functional Android app that monitors Indian Railways seat availability and fires a loud alarm when seats become available.

---

## Build Requirements

- **Android Studio Hedgehog (2023.1)** or later
- **JDK 17**
- **Android SDK 34** (target), **SDK 26** (minimum)
- Internet connection (Gradle dependencies are downloaded on first build)

## Build Instructions

1. Open Android Studio → **File → Open** → select `Seat_Reminder/`
2. Let Gradle sync finish (first sync downloads ~200 MB of dependencies)
3. Connect a device or start an emulator (Android 8.0+)
4. Click **Run ▶**

To build a release APK:
```
./gradlew assembleRelease
```
The signed APK will be at `app/build/outputs/apk/release/`.

---

## Architecture

```
data/
  db/           Room entities, DAOs, Database, DatabaseModule
  model/        AlertConfig, CheckResult (Room @Entity classes)
  network/      Retrofit API service, response models, parser, NetworkModule
  repository/   AlertRepository — single truth source, wraps DB + network

domain/
  usecase/      GetAllAlertsUseCase, SaveAlertUseCase, DeleteAlertUseCase,
                CheckSeatAvailabilityUseCase

presentation/
  dashboard/    DashboardScreen + DashboardViewModel
  addedit/      AddEditAlertScreen + AddEditViewModel
  detail/       AlertDetailScreen + AlertDetailViewModel
  settings/     SettingsScreen + SettingsViewModel (DataStore-backed)
  theme/        Material3 color/type/theme

alarm/          AlarmActivity (full-screen lock-screen alarm), AlarmHelper
receiver/       BootReceiver (reschedule on reboot), AlarmDismissReceiver
worker/         SeatCheckWorker (CoroutineWorker), WorkerScheduler, WorkerModule
widget/         SeatAlertWidget (4×1 home screen widget)
util/           DateUtils, NotificationUtils, Extensions
```

---

## Adding a New API Source

All API logic lives in `data/repository/AlertRepository.kt`.

1. Add a new Retrofit interface in `data/network/` (or use `OkHttpClient` directly for HTML scraping).
2. In `AlertRepository.checkSeatAvailability()`, try your new source before or after the existing ones.
3. Parse the response string with `SeatAvailabilityParser.parse(rawString)`.
4. Return the `SeatAvailability` object — the worker handles alarming automatically.

Example (adding a hypothetical API):
```kotlin
private suspend fun tryMyNewApi(alert: AlertConfig): SeatAvailability {
    val response = myNewApiService.getSeats(alert.trainNumber, ...)
    return if (response.isSuccessful) {
        SeatAvailabilityParser.parse(response.body()?.availabilityString ?: "UNKNOWN")
    } else {
        SeatAvailability(AvailabilityStatus.UNKNOWN, 0, "UNKNOWN")
    }
}
```
Then call `tryMyNewApi(alert)` as a fallback in `checkSeatAvailability()`.

---

## Permissions

| Permission | Why |
|---|---|
| INTERNET | API calls to check seat availability |
| RECEIVE_BOOT_COMPLETED | Re-schedule workers after reboot |
| VIBRATE | Alarm vibration pattern |
| WAKE_LOCK | Keep CPU awake while alarm fires |
| USE_FULL_SCREEN_INTENT | Show alarm on lock screen |
| FOREGROUND_SERVICE | WorkManager foreground service on Android 12+ |
| POST_NOTIFICATIONS | Show alarm notifications (Android 13+, requested at runtime) |
| SCHEDULE_EXACT_ALARM | Exact alarm scheduling (Android 12+) |

---

## Deep Links

Open a specific alert directly:
```
adb shell am start -W -a android.intent.action.VIEW \
  -d "trainseat://alert/1" com.trainseat.app
```

---

## Running Tests

```
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device/emulator required)
```

---

## Notes

- The app uses three API fallback sources: ixigo → erail.in → confirmtkt. The ixigo endpoint is tried first; if it fails or returns no data the erail HTML scrape is attempted.
- WorkManager minimum interval is 15 minutes (Android OS enforced). The UI enforces this floor.
- On Android 12+, if `SCHEDULE_EXACT_ALARM` is denied, a banner appears on the Dashboard with a link to the system settings.
- Alarm notifications use `IMPORTANCE_HIGH` + `setBypassDnd(true)` to cut through Do-Not-Disturb.

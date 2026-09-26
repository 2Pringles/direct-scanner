# Direct Scanner

An Android app that scans for nearby Wi-Fi networks and displays only the
ones matching a specific naming scheme: SSIDs starting with the exact,
all-caps prefix `DIRECT-` that also look like printer Wi-Fi Direct broadcasts
(e.g. `DIRECT-InkJet-HP-PRINTER`).

Matching networks are shown full-screen, one per grid cell, with no
scrolling — the more of them are found, the more the grid subdivides to fit
them all. Each network gets its own bold, distinct color (stable for the
session), and its box grows bigger and brighter the stronger its signal
(i.e. the closer you are to it).

## Requirements

- Android Studio (Koala/2024.1 or newer recommended)
- JDK 17 (Android Studio bundles one)
- A **physical Android device**, API 26+ — emulators generally don't
  produce real Wi-Fi scan results.

## Building with only a phone (no computer)

This project includes a GitHub Actions workflow (`.github/workflows/build.yml`)
that builds the debug APK in the cloud on every push — you never need
Android Studio or a desktop at all. Steps, all doable from an Android phone:

1. **Install Termux** (get it from F-Droid, not the Play Store version,
   which is outdated) — this gives you a terminal on your phone.
2. Save `DirectScanner.zip` to your phone (e.g. your Downloads folder).
3. In Termux:
   ```
   pkg update && pkg install git unzip
   termux-setup-storage
   cd ~
   unzip /sdcard/Download/DirectScanner.zip -d DirectScanner
   cd DirectScanner
   ```
   (adjust the unzip path if your browser saved it somewhere else)
4. On github.com (mobile browser is fine), create a new **empty** repository
   (don't add a README/license) — e.g. `direct-scanner`.
5. On github.com, generate a Personal Access Token: **Settings → Developer
   settings → Personal access tokens → Tokens (classic)**, scope `repo`.
   You'll use this as your password in the next step.
6. Back in Termux, push the project:
   ```
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/direct-scanner.git
   git push -u origin main
   ```
   When prompted for a password, paste the token from step 5.
7. On github.com, open your repo's **Actions** tab. The "Build APK" workflow
   starts automatically after the push. Wait for it to finish (a few
   minutes), open the completed run, and download the
   `direct-scanner-debug-apk` artifact (it's a zip containing the `.apk`).
8. Unzip that (Termux's `unzip`, or any file manager app), then tap the
   `.apk` file to install it. Android will ask you to allow installs from
   whichever app you opened it with ("install unknown apps") — allow it,
   then install.

From then on, any time you edit code and `git push`, a fresh APK will be
built automatically — just repeat step 7–8 to grab the new one.

## Opening & running on a computer

If you get access to a Mac or Linux machine later, Android Studio runs
natively on both (this isn't Windows-only) — just:

1. Open this folder (`DirectScanner/`) in Android Studio as an existing project.
2. Let Gradle sync (it will download dependencies the first time).
3. Connect a physical device with Wi-Fi and USB debugging on, and hit Run.
4. Grant the permission prompt (location on Android 12 and below, "Nearby
   Wi-Fi devices" on Android 13+) — this is required by Android for any app
   reading Wi-Fi scan results, it is not used for anything else here.

## Where to customize

- **Naming rule** — `NetworkFilter.kt`. The `DIRECT-` prefix check is exact
  and case-sensitive. The list of printer keywords (`PRINTER_KEYWORDS`) is
  what's used to recognize "apparent printer lingo" after the prefix — add
  your own terms there if a network isn't matching.
- **Scan speeds** — `WifiScanManager.kt`, the `ScanSpeed` enum. Adjust the
  three interval values (`BATTERY_SAVER`, `NORMAL`, `TURBO`) in milliseconds.
- **Proximity look** — `ui/MainScreen.kt`, `proximityFraction()` and
  `NetworkCard()`. Controls the dBm range used for "far" vs "close", and how
  strongly that maps to box size/brightness/font size.
- **Colors** — `ColorAssigner.kt` assigns each SSID a hue using golden-angle
  rotation, which keeps arbitrarily many networks visually distinct.

## Important: Android's built-in scan throttling

Since Android 9, the OS itself limits how often any single app can request
a Wi-Fi scan — roughly 4 requests per 2-minute window in the foreground,
fewer in the background. This means:

- **Turbo mode won't necessarily give you a scan every few seconds on every
  device.** The app requests scans at the configured interval, but Android
  may silently ignore requests that come in too fast and just keep returning
  the last cached results until the throttle window passes.
- For testing/personal use, you can disable this on your own device via
  **Settings → Developer options → Wi-Fi scan throttling** (turn it off).
  This is a system-wide setting, not something the app can control
  programmatically.
- This throttling is a platform limitation, not a bug in the app — there's
  no way for a normal (non-system) app to bypass it.

## Notes

- No launcher icon is bundled (the manifest omits `android:icon`), so the
  device's default icon will show. Drop your own into
  `app/src/main/res/mipmap-*` and reference it in
  `AndroidManifest.xml` if you want a custom one.
- This project was hand-written and has not been compiled in this
  environment (no Android SDK/emulator available here) — it should build
  cleanly in Android Studio, but if you hit a small Gradle/AGP version
  mismatch on your machine, letting Android Studio's "Upgrade Assistant"
  adjust versions should resolve it.

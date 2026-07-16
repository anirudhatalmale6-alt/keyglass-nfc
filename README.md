# KEY GLASS

Native Android app adapted from LoopO NFC. Reads and writes a **Base Code** to an
NFC tag and lets you store short account **identifiers** locally. 100% offline —
no Internet permission, no analytics, no cloud.

## Tabs

1. **Base Code** — tap *READ WATCH*, present the NFC tag, and the Base Code is
   copied into a secure in-app clipboard (never shown on screen, never written to
   the Android system clipboard). Auto-clears after 30 seconds, or tap
   *CLEAR CLIPBOARD*.
2. **Identifiers** — read-only list of your identifiers (e.g. `EM : email`).
3. **Setup** — write the Base Code to a tag (with an optional irreversible
   *Write-protect*), and add / edit / delete identifiers.

## Tech

- Kotlin, single Activity + ViewPager2 with 3 fragments
- NFC NDEF text read/write (`android.nfc`)
- Room database for identifiers (local only)
- minSdk 26 (Android 8.0), targetSdk 34
- Permissions: `NFC`, `VIBRATE` only — **no** `INTERNET`

## Build

CI builds the debug APK automatically (see `.github/workflows/build.yml`).
Locally:

```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (platform 34, build-tools 34.0.0).

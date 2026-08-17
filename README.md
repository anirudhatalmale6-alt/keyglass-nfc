# LOOPO 3FA

Native Android app adapted from LoopO NFC. Reads and writes a **Base Code** to an
NFC TAG and lets you store short account **identifiers** locally. 100% offline —
no Internet permission, no analytics, no cloud.

(Formerly delivered as *KEY GLASS*; renamed to **LOOPO 3FA** in v1.0.)

## Tabs

1. **Base Code** — tap *READ NFC TAG*, present the TAG, and the Base Code appears
   in the frame (partially masked, e.g. `7hD********GZ`, when the masking option is
   on). Tap *COPY* to put the full code on the clipboard so it can be pasted into
   any field, and *CLEAR CLIPBOARD* when you are done. Nothing clears itself
   automatically. On Android 13+ the clip is flagged sensitive, so the system keeps
   it out of the paste preview and clipboard history.
2. **Identifiers** — read-only list of your identifiers (e.g. `EM : email`).
3. **Setup** — write the Base Code to a TAG (with an optional irreversible
   *Write-protect*), switch the *Mask Base Code* option on/off, and add / edit /
   delete identifiers.

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

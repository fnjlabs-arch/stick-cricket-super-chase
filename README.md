# Stick Cricket Super Chase

A fast, casual 6-ball stickman cricket batting game for Android — timing-based shots (hook, pull, cover drive, lofted sweep), 1/2/3-over match modes, daily challenges and rewards, cosmetics collection, local career stats, power-ups (Golden Bat, Slow Motion), sounds and haptics. Fully offline.

- **Package:** `com.sadsha.stickcricket`
- **Version:** 1.0 (versionCode 1)
- **Tech:** Kotlin + Jetpack Compose, Room + DataStore, minSdk 24 / targetSdk 36
- **Free v1:** no ads, no in-app purchases. Bonus coins, second chances, and power-up refills are granted free inside the game.

## Build

```bash
./gradlew assembleDebug     # test APK -> app/build/outputs/apk/debug/
./gradlew bundleRelease     # release AAB (needs the release keystore, see below)
```

GitHub Actions (`.github/workflows/`):
- **Build debug APK** — runs on push to `main` and manually. Produces a debug APK artifact.
- **Build release AAB** — manual only. Needs these repository secrets (Settings → Secrets → Actions):
  - `KEYSTORE_BASE64` — base64 of the release `.keystore` file
  - `KEYSTORE_PASSWORD` — keystore password
  - `KEY_ALIAS` — key alias
  - `KEY_PASSWORD` — key password

## Play Store

- Listing copy: `PLAY_LISTING.md`
- Privacy policy: `PRIVACY_POLICY.md` / `privacy_policy.html` (host it and put the URL in the listing — still needed)
- Store assets: `store_assets/` (512×512 icon, 1024×500 feature graphic, screenshots)
- Support email: sadsha4334@gmail.com

## Notes

- The game was previously named "6 Ball Stick Cricket"; the canonical name is now **Stick Cricket Super Chase** everywhere. Note: "Stick Cricket" is an existing famous game brand, so there is a trademark risk on the name — flagged to the owner.
- Screenshots in `store_assets/` are 768×1376 (9:16). Retake at 1080×1920 from the built game for best store quality.

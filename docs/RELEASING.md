# Release procedure

Publication convention:

- Source: `li-yifei/PixelWatchBatteryDrainFix`, GitHub tag `v0.5.0`.
- Application ID: `io.github.li_yifei.pixelwatchbatterydrainfix`.
- Xposed listing: package-named repository under Xposed-Modules-Repo.
- Xposed release tag: `5-0.5.0`, title `0.5.0`.
- Publish identical signed APK and SHA256SUMS to both releases.
- Listing repository contains SUMMARY and description documents, not source code.

## Build and sign

Use JDK 17+ and Android SDK 35. Set ANDROID_HOME or local.properties.
Run `./gradlew :app:assembleDebug :app:assembleRelease :app:lint`.

Release signing reads RELEASE_KEYSTORE, RELEASE_KEY_ALIAS,
RELEASE_STORE_PASSWORD and RELEASE_KEY_PASSWORD from the environment.
Obtain the release key from secure local storage.
Never commit key material, passwords, debug-signed releases, or raw device logs.
Without all four variables, the release is unsigned and must not be published.

Verify with Android build-tools `apksigner verify --verbose --print-certs`.
Check application ID, version code, Xposed metadata and companion scope.
Install and test the release package on a compatible device.

## Submission

Create an issue in Xposed-Modules-Repo/submission titled
`[submission] io.github.li_yifei.pixelwatchbatterydrainfix` using SUBMISSION.md.
Accept the bot's repository invitation, set its description to the module name,
upload listing documents and the signed release. Verify the indexed page.

The first public release should be experimental; full battery-life and
cross-device feature regression checks remain incomplete.

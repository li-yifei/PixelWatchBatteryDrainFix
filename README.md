# Pixel Watch Battery Drain Fix

English | [简体中文](README.zh-CN.md)

**Does your Pixel Watch battery drain rapidly when connected to your rooted phone—even with the screen off?**

This LSPosed module targets a phone–watch communication loop that repeatedly wakes the watch. In the tested setup, blocking the affected channel stopped the repeated wake-ups.

**Install on your rooted phone. Your watch does not need root.**

## Symptoms this targets

- Pixel Watch / Wear OS battery drain with repeated companion transport requests.
- Frequent `Wear_Transport` or `wearChannelApiRetransmissionQueue` wake locks while idle.
- CDM secure-transport retries in a rooted-phone setup with LSPosed.

Root alone does not establish the cause. This workaround addresses one communication problem. It does not fix every battery drain issue or make attestation pass.

## Trade-offs

The module blocks Android Companion Device Manager (CDM) secure data transport. Permission sync and features using this channel may stop working. Pixel System Intelligence requested the channel in our logs; the exact AI features affected are unverified.

Notifications, calls and Fitbit sync require further compatibility testing. Battery-life improvement depends on the cause of the drain and remains under evaluation.

## Compatibility

| Component | Tested / required |
| --- | --- |
| Watch | Pixel Watch 5 Wi-Fi; PW4 unverified with this release |
| Phone | Rooted Android phone; Android 17 compatibility under evaluation |
| Pixel Watch companion app | **5.0.0.958206140** |
| Framework | Modern Xposed API 102; built with API 102 |

**Experimental and version-specific.** A private companion method is hooked; app updates can break compatibility. Magisk, other phones and other system versions are untested.

## Install

1. Install the release APK on the phone.
2. Enable the module in LSPosed; select Google Pixel Watch (`com.google.android.apps.wear.companion`).
3. Restart the companion processes or reboot the phone.
4. Verify `protective channel entry hooks=1` and `declined CDM channel creation` in module logs.

To restore CDM features, disable this module and restart the companion app or phone.

## How it works

Declines `/cdm/channel/secure_transport` creation for both phone- and watch-originated requests, and limits repeated callbacks. Certificate verification and attestation results remain unchanged.

This experimental release requires further on-device validation and full battery-life testing.

## Troubleshooting

Include device models, system versions, companion version, framework version and short redacted excerpts for `WatchCdmBreaker`, `WearCDM`, and `CDM_SecureChannel`. Remove device identifiers and personal data. Compare ordinary battery use with wireless ADB switched off.

## Build

Use JDK 17+, Android SDK 35 and `./gradlew :app:assembleDebug :app:lint`. See [release instructions](docs/RELEASING.md).

License: GPL-3.0-or-later.

# Pixel Watch Battery Drain Fix

An experimental LSPosed workaround for excessive Pixel Watch battery drain caused by repeated companion transport wake-ups when paired with a rooted Android phone. Installed on the phone; the watch needs no root.

- Application ID: `io.github.li_yifei.pixelwatchbatterydrainfix`
- Author: https://github.com/li-yifei
- Source: https://github.com/li-yifei/PixelWatchBatteryDrainFix
- Releases: https://github.com/li-yifei/PixelWatchBatteryDrainFix/releases
- Support: https://github.com/li-yifei/PixelWatchBatteryDrainFix/issues
- License: GPL-3.0-or-later
- Framework: modern Xposed API 102; built with API 102.
- Compatibility target: Pixel Watch 5 Wi-Fi, rooted Android phone, companion 5.0.0.958206140.

Disables CDM secure transport. Permission sync and related cross-device features may be unavailable. It does not bypass attestation. Other causes of battery drain remain possible; full-day battery improvement and public-package device validation are pending.

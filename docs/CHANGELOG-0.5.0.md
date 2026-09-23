# 0.5.0 — Experimental

First public package of Pixel Watch Battery Drain Fix.

- Blocks the CDM secure-transport creation path responsible for repeated wake-ups in the tested setup.
- Adds transport request limiting and deactivation handling.
- Public application ID: `io.github.li_yifei.pixelwatchbatterydrainfix`.
- Requires modern Xposed API 102 and companion app 5.0.0.958206140.

Install on the rooted phone; the watch needs no root.

**Trade-off:** CDM secure transport is disabled. Permission sync and related cross-device smart features may be unavailable. This does not make attestation pass or address every cause of battery drain. Further device, full-day battery and notification/call/health-sync testing remains pending.

首个公开实验版本。安装在 Root 手机上，手表无需 Root。本模块会停用 CDM 安全传输，权限同步及相关智能联动可能受影响。设备验证与完整续航测试仍待完成。

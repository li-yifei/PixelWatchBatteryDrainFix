# Pixel Watch Battery Drain Fix

[English](README.md) | 简体中文

**Pixel Watch 连接 Root 手机后掉电很快，熄屏待机也耗电？**

这个 LSPosed 模块针对手机与手表之间反复唤醒手表的通信循环。在已测试设备上，阻止相关通道建立后，重复唤醒停止。

**模块装在 Root 手机上，手表无需 Root。**

## 适用症状

- Pixel Watch / Wear OS 连接手机后异常耗电，同时出现重复传输请求。
- 熄屏时频繁出现 `Wear_Transport`、`wearChannelApiRetransmissionQueue` 唤醒锁。
- CDM 安全通信重试。

仅有 Root 无法确定耗电原因。本模块只处理一种通信循环，也不会让 attestation 验证通过。

## 功能代价

模块关闭 CDM 安全数据传输，权限同步及使用该通道的跨设备功能可能失效。Pixel System Intelligence 会申请该通道，具体涉及哪些 AI 功能仍待确认。

通知、通话、Fitbit 同步仍需兼容性验证。续航改善取决于具体耗电原因，完整续航测试仍在进行。

## 兼容性

当前适配目标：Pixel Watch 5 Wi-Fi、Root Android 手机、Pixel Watch 配套应用 **5.0.0.958206140**。Android 17 兼容性仍在验证。框架需要支持现代 API 102。

**实验版本，依赖配套应用的私有方法。** 应用更新可能导致失效。PW4、其他手机和系统待验证。

## 安装

1. 在手机安装 Release APK。
2. 在 LSPosed 启用模块，作用域选择 Google Pixel Watch（`com.google.android.apps.wear.companion`）。
3. 重启配套应用进程或手机。
4. 核对日志中的 `protective channel entry hooks=1` 和 `declined CDM channel creation`。

需要恢复 CDM 功能时，关闭模块并重启配套应用或手机。

## 原理与反馈

模块阻止 `/cdm/channel/secure_transport` 建链，并限制重复请求；证书与 attestation 结果保持原样。

反馈请注明设备、系统、配套应用与 LSPosed 版本，提供脱敏的 `WatchCdmBreaker`、`WearCDM`、`CDM_SecureChannel` 日志。日常耗电对照建议关闭无线 ADB。

构建和签名见 [发布说明](docs/RELEASING.md)。许可证 GPL-3.0-or-later。

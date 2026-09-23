# Implementation

## Protective mode

On Companion 5.0.0.958206140, repeated transport deactivation requests were
ignored while the watch kept its Wear_Transport wake lock. The module now also
declines CDM secure-channel creation through the verified `wsa.e(String,
AssociationInfo, Continuation)` entry, covering watch-originated requests.
This is a protective workaround: CDM system-data transport is unavailable while
enabled. It does not establish that attestation passes. Bluetooth pairing and
other Wear channels are outside this hook; their user-facing functions still
need regression checks.

Repeated activation callbacks are limited independently of attach attempts.
Deactivation detaches transport and reports RESULT_DEACTIVATED; a late attach
after deactivation closes its supplied streams. A subsequent activation clears
the deactivated marker.

The private entry hook is tied to the inspected Companion build and must be
revalidated after updates. Confirm `protective channel entry hooks=1` in logs.
Disable the module and restart Companion to restore the original behavior.

## Original attach circuit breaker

An LSPosed modern-API module that stops a Pixel Watch Companion Device Manager retry storm.

The APK uses `io.github.libxposed:api:102.0.0`, with `minApiVersion=102` and
`targetApiVersion=102`. Its module metadata, entry point, and static scope live under
`META-INF/xposed/`; it contains no legacy `XposedBridge` API or `assets/xposed_init` entry.

It hooks only `com.google.android.apps.wear.companion:persistent` and observes real calls to
`CompanionDeviceManager.attachSystemDataTransport`. Three attaches for the same association in
15 seconds open a five-minute cooldown. During the cooldown, incoming
`REQUEST_TRANSPORT / OP_ACTIVATE` requests are no-ops.

The module does not hook GMS attestation, Android Keystore, certificate chains, or verification
results. The expected side effect is a pause in CDM system-data sync for the affected watch during
the cooldown. Normal Bluetooth pairing and the broader Wearable Data Layer have separate paths.

The static scope admits only `com.google.android.apps.wear.companion`; its persistent process is
selected automatically by the module. Verify behavior with:

```sh
adb logcat -s WatchCdmBreaker WearCDM CDM_CompanionTransportManager CDM_SecureChannel
```

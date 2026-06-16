## 0.0.4

* `ScreenEventService` no longer crashes the host app when `startForeground()` fails on Android 14+ (e.g. missing FGS permissions or Play Console special-use declaration); the service stops itself and logs the error instead
* Fixed foreground service type mismatch on API 29–33: removed `DATA_SYNC` runtime type that did not match the manifest’s `specialUse|systemExempted` declaration; pre-API-34 devices now rely on the manifest declaration
* `lockScreen()` catches `SecurityException` from `DevicePolicyManager.lockNow()` and surfaces a `LOCK_FAILED` platform error instead of crashing when device admin is revoked mid-call

## 0.0.3

* Screen on/off events are now delivered by a dedicated foreground service (`ScreenEventService`) so they keep firing when the host app is backgrounded or running under background / FGS restrictions
* Multi-source screen state detection: `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` broadcasts + `DisplayManager` display-state changes + a `PowerManager.isInteractive` polling fallback, de-duplicated into a single event stream
* Adaptive foreground service type on Android 14+: `systemExempted` when the app is the device owner (kiosk), `specialUse` otherwise, with the required `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` declared in the manifest
* Persistent low-importance notification ("Screen monitor active") keeps the monitoring service alive
* New Android permissions declared: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, `POST_NOTIFICATIONS`
* Errors starting the foreground service are now surfaced on the event stream as `FGS_START_FAILED` instead of being silently dropped
* Example app: request `POST_NOTIFICATIONS` on startup, show notification permission status, and tap the "Last screen event" label to see the last 10 events with timestamps

## 0.0.2

* Add screen state query (`isScreenOn`)
* Add screen on/off event stream (`onScreenStateChanged`)
* Example app updated to display screen state and listen to events

## 0.0.1

* Initial Android plugin code
* Request admin permission
* Request if app has admin permission
* Lock device logic

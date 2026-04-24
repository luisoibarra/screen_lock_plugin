# Screen Lock Plugin

A Flutter plugin that allows you to programmatically lock the Android device screen.

## Features

- Lock the device screen programmatically
- Check device admin permission status
- Request device admin permissions
- Check current screen state (`isScreenOn`)
- Stream screen on/off events (`onScreenStateChanged`) from a dedicated foreground service so events keep firing while the app is backgrounded
- Multi-source screen-state detection (broadcast + `DisplayManager` + `PowerManager` polling fallback), de-duplicated into a single stream
- Adaptive foreground service type: `systemExempted` for device-owner kiosks, `specialUse` otherwise
- Simple and easy-to-use API

## Platform Support

| Platform | Supported |
| -------- | --------- |
| Android  | ✅        |
| iOS      | ❌        |
| Web      | ❌        |
| Windows  | ❌        |
| macOS    | ❌        |
| Linux    | ❌        |

## Android Setup

No additional manifest setup is required. The plugin automatically configures the necessary permissions, the device admin receiver, and the `ScreenEventService` foreground service used to deliver screen on/off events.

### Notification permission (Android 13+)

While `onScreenStateChanged()` has an active listener, the plugin starts a foreground service that posts a persistent low-importance notification ("Screen monitor active"). The service still runs if the user denies `POST_NOTIFICATIONS`, but the notification will be suppressed. To make the service visible and to match user expectations, request the notification permission from your app before subscribing, for example with [`permission_handler`](https://pub.dev/packages/permission_handler):

```dart
import 'package:permission_handler/permission_handler.dart';

await Permission.notification.request();
```

## Usage

### Import the package

```dart
import 'package:screen_lock_plugin/screen_lock_plugin.dart';
```

### Create an instance

```dart
final screenLockPlugin = ScreenLockPlugin();
```

### Check if device admin is enabled

```dart
bool? isEnabled = await screenLockPlugin.isDeviceAdminEnabled();
if (isEnabled == true) {
  print('Device admin is enabled');
} else {
  print('Device admin is not enabled');
}
```

### Request device admin permissions

```dart
await screenLockPlugin.requestDeviceAdmin();
```

This will show a system dialog asking the user to grant device admin permissions to your app.

### Lock the screen

```dart
bool? result = await screenLockPlugin.lockScreen();
if (result == true) {
  print('Screen locked successfully');
} else {
  print('Failed to lock screen. Device admin may not be enabled.');
}
```

### Check the current screen state

```dart
bool? isOn = await screenLockPlugin.isScreenOn();
print('Screen is currently ${isOn == true ? "ON" : "OFF"}');
```

### Listen for screen on/off events

```dart
final subscription = screenLockPlugin.onScreenStateChanged().listen(
  (event) {
    // event is either 'SCREEN_ON' or 'SCREEN_OFF'
    print('Screen event: $event');
  },
  onError: (Object error) {
    // e.g. 'FGS_START_FAILED' if the foreground service could not start
    print('Screen event error: $error');
  },
);

// Later, when you no longer need events:
await subscription.cancel();
```

The first active listener starts `ScreenEventService` as a foreground service and shows an ongoing notification. When the last listener is cancelled, the service is stopped.

## How It Works

This plugin uses Android's `DevicePolicyManager` API to lock the screen and a dedicated foreground service to monitor the display state. Here's what happens:

1. **Device Admin Permissions**: The app must be registered as a device administrator to lock the screen. This is a security requirement by Android.

2. **User Consent**: Users must explicitly grant device admin permissions through a system dialog. This cannot be done automatically.

3. **Screen Lock**: Once permissions are granted, the plugin can lock the screen immediately using `DevicePolicyManager.lockNow()`.

4. **Screen Event Monitoring**: While a Dart listener is attached to `onScreenStateChanged()`, the plugin runs `ScreenEventService` as a foreground service. It combines three independent signals and emits only real transitions:

   - `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` broadcasts
   - `DisplayManager.DisplayListener` state changes (catches AOD / doze transitions that do not broadcast on some OEMs)
   - A periodic `PowerManager.isInteractive` poll as a safety net against broadcast throttling

5. **Adaptive Foreground Service Type** (Android 14+): If the app is provisioned as the device owner, the service starts with `FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED` for the broadest kiosk exemption. Otherwise it uses `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`, backed by the `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` declared in the manifest.

## Important Notes

- **User Permission Required**: Users must manually grant device admin permissions. This is a security feature and cannot be bypassed.

- **Revoking Permissions**: Users can revoke device admin permissions at any time through:

  - Settings → Security → Device administrators

- **Uninstalling**: If users want to uninstall your app, they must first disable device admin permissions in Settings.

- **Best Practices**:
  - Always check if device admin is enabled before attempting to lock the screen
  - Provide clear UI feedback about permission status
  - Explain why your app needs this permission

## Permissions

The plugin automatically adds the following permissions to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

- `BIND_DEVICE_ADMIN` — required to lock the screen via `DevicePolicyManager`.
- `FOREGROUND_SERVICE` (+ the `SPECIAL_USE` / `SYSTEM_EXEMPTED` subtypes) — required to run `ScreenEventService` so screen events are delivered reliably in the background.
- `POST_NOTIFICATIONS` — runtime permission on Android 13+ for the service's ongoing notification. The service still runs if denied, but the notification is suppressed.

## Troubleshooting

### Screen doesn't lock when button is pressed

- Verify device admin is enabled by checking `isDeviceAdminEnabled()`
- If not enabled, call `requestDeviceAdmin()` to prompt the user

### App won't uninstall

- Users must disable device admin permissions before uninstalling
- Go to Settings → Security → Device administrators → Disable your app

### `onScreenStateChanged()` never emits events

- Make sure a listener is attached — the foreground service only starts while a subscription is active.
- Check the system logs for `ScreenLockPlugin` / `ScreenEventService` tags. Startup failures are logged there and are also surfaced on the stream as an error with code `FGS_START_FAILED`.
- On Android 13+, grant `POST_NOTIFICATIONS` so you can see the "Screen monitor active" notification and confirm the service is running.
- Some OEMs aggressively restrict background execution. Provisioning the app as a device owner (kiosk) lets the service use `FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED` and bypass most of those restrictions.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

If you encounter any issues or have questions, please file an issue on the GitHub repository.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes in each version.

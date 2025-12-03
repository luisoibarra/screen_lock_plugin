# Screen Lock Plugin

A Flutter plugin that allows you to programmatically lock the Android device screen.

## Features

- Lock the device screen programmatically
- Check device admin permission status
- Request device admin permissions
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

No additional setup is required. The plugin automatically configures the necessary permissions and device admin receiver.

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

## How It Works

This plugin uses Android's `DevicePolicyManager` API to lock the screen. Here's what happens:

1. **Device Admin Permissions**: The app must be registered as a device administrator to lock the screen. This is a security requirement by Android.

2. **User Consent**: Users must explicitly grant device admin permissions through a system dialog. This cannot be done automatically.

3. **Screen Lock**: Once permissions are granted, the plugin can lock the screen immediately using `DevicePolicyManager.lockNow()`.

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

The plugin automatically adds the following permission to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />
```

## Troubleshooting

### Screen doesn't lock when button is pressed

- Verify device admin is enabled by checking `isDeviceAdminEnabled()`
- If not enabled, call `requestDeviceAdmin()` to prompt the user

### App won't uninstall

- Users must disable device admin permissions before uninstalling
- Go to Settings → Security → Device administrators → Disable your app

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

If you encounter any issues or have questions, please file an issue on the GitHub repository.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes in each version.

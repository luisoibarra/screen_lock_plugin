import 'screen_lock_plugin_platform_interface.dart';

class ScreenLockPlugin {
  Future<bool?> lockScreen() {
    return ScreenLockPluginPlatform.instance.lockScreen();
  }

  Future<bool?> isDeviceAdminEnabled() {
    return ScreenLockPluginPlatform.instance.isDeviceAdminEnabled();
  }

  Future<void> requestDeviceAdmin() {
    return ScreenLockPluginPlatform.instance.requestDeviceAdmin();
  }

  Future<bool?> isScreenOn() {
    return ScreenLockPluginPlatform.instance.isScreenOn();
  }

  Stream<String> onScreenStateChanged() {
    return ScreenLockPluginPlatform.instance.onScreenStateChanged();
  }
}

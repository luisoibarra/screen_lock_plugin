import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'screen_lock_plugin_platform_interface.dart';

class MethodChannelScreenLockPlugin extends ScreenLockPluginPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('screen_lock_plugin');

  @override
  Future<bool?> lockScreen() async {
    final result = await methodChannel.invokeMethod<bool>('lockScreen');
    return result;
  }

  @override
  Future<bool?> isDeviceAdminEnabled() async {
    final result = await methodChannel.invokeMethod<bool>(
      'isDeviceAdminEnabled',
    );
    return result;
  }

  @override
  Future<void> requestDeviceAdmin() async {
    await methodChannel.invokeMethod<void>('requestDeviceAdmin');
  }
}

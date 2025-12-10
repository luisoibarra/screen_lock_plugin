import 'dart:async';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'screen_lock_plugin_method_channel.dart';

abstract class ScreenLockPluginPlatform extends PlatformInterface {
  ScreenLockPluginPlatform() : super(token: _token);

  static final Object _token = Object();
  static ScreenLockPluginPlatform _instance = MethodChannelScreenLockPlugin();

  static ScreenLockPluginPlatform get instance => _instance;

  static set instance(ScreenLockPluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<bool?> lockScreen() {
    throw UnimplementedError('lockScreen() has not been implemented.');
  }

  Future<bool?> isDeviceAdminEnabled() {
    throw UnimplementedError(
      'isDeviceAdminEnabled() has not been implemented.',
    );
  }

  Future<void> requestDeviceAdmin() {
    throw UnimplementedError('requestDeviceAdmin() has not been implemented.');
  }

  Future<bool?> isScreenOn() {
    throw UnimplementedError('isScreenOn() has not been implemented.');
  }

  Stream<String> onScreenStateChanged() {
    throw UnimplementedError('onScreenStateChanged() has not been implemented.');
  }
}

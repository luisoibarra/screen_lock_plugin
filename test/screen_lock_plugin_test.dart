import 'package:flutter_test/flutter_test.dart';
import 'package:screen_lock_plugin/screen_lock_plugin.dart';
import 'package:screen_lock_plugin/screen_lock_plugin_platform_interface.dart';
import 'package:screen_lock_plugin/screen_lock_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockScreenLockPluginPlatform
    with MockPlatformInterfaceMixin
    implements ScreenLockPluginPlatform {
  @override
  Future<bool?> isDeviceAdminEnabled() {
    // TODO: implement isDeviceAdminEnabled
    throw UnimplementedError();
  }

  @override
  Future<bool?> lockScreen() {
    // TODO: implement lockScreen
    throw UnimplementedError();
  }

  @override
  Future<void> requestDeviceAdmin() {
    // TODO: implement requestDeviceAdmin
    throw UnimplementedError();
  }
}

void main() {
  final ScreenLockPluginPlatform initialPlatform =
      ScreenLockPluginPlatform.instance;

  test('$MethodChannelScreenLockPlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelScreenLockPlugin>());
  });

  test('getPlatformVersion', () async {
    ScreenLockPlugin screenLockPlugin = ScreenLockPlugin();
    MockScreenLockPluginPlatform fakePlatform = MockScreenLockPluginPlatform();
    ScreenLockPluginPlatform.instance = fakePlatform;
  });
}

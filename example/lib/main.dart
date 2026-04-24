import 'dart:async';
import 'dart:developer';
import 'dart:io' show Platform;

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:screen_lock_plugin/screen_lock_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(home: ScreenLockHomePage());
  }
}

class ScreenLockHomePage extends StatefulWidget {
  const ScreenLockHomePage({super.key});

  @override
  State<ScreenLockHomePage> createState() => _ScreenLockHomePageState();
}

class _ScreenLockHomePageState extends State<ScreenLockHomePage> {
  final _screenLockPlugin = ScreenLockPlugin();
  bool _isAdminEnabled = false;
  bool? _isScreenOn;
  String _lastScreenEvent = 'None';
  static const int _maxRecentScreenEvents = 10;
  final List<_ScreenEventEntry> _recentScreenEvents = <_ScreenEventEntry>[];
  PermissionStatus _notificationStatus = PermissionStatus.denied;
  StreamSubscription<String>? _screenEventSub;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    // Request POST_NOTIFICATIONS before subscribing so the foreground-service
    // notification is visible on Android 13+. The FGS still runs when denied,
    // but the "Screen monitor active" notification would otherwise be
    // suppressed, which makes verification during testing misleading.
    await _requestNotificationPermission();
    await _refreshPermissionStatuses();
    await _checkScreenState();
    await _checkAdminStatus();
    _subscribeToScreenEvents();
  }

  Future<void> _requestNotificationPermission() async {
    if (!Platform.isAndroid) return;
    log(
      'Requesting POST_NOTIFICATIONS permission',
      name: 'ScreenLockPluginExample',
    );
    final status = await Permission.notification.request();
    log('POST_NOTIFICATIONS status: $status', name: 'ScreenLockPluginExample');
  }

  Future<void> _refreshPermissionStatuses() async {
    if (!Platform.isAndroid) return;
    final notification = await Permission.notification.status;
    if (!mounted) return;
    setState(() {
      _notificationStatus = notification;
    });
  }

  Future<void> _checkAdminStatus() async {
    log('Checking device admin status', name: 'ScreenLockPluginExample');
    final isEnabled = await _screenLockPlugin.isDeviceAdminEnabled();
    log('Device admin enabled: $isEnabled', name: 'ScreenLockPluginExample');
    if (!mounted) return;
    setState(() {
      _isAdminEnabled = isEnabled ?? false;
    });
  }

  Future<void> _checkScreenState() async {
    log('Checking screen state', name: 'ScreenLockPluginExample');
    final isOn = await _screenLockPlugin.isScreenOn();
    log('Screen is on: $isOn', name: 'ScreenLockPluginExample');
    if (!mounted) return;
    setState(() {
      _isScreenOn = isOn;
    });
  }

  void _subscribeToScreenEvents() {
    log('Subscribing to screen state changes', name: 'ScreenLockPluginExample');
    _screenEventSub?.cancel();
    _screenEventSub = _screenLockPlugin.onScreenStateChanged().listen(
      (event) {
        log('Screen event received: $event', name: 'ScreenLockPluginExample');
        if (!mounted) return;
        setState(() {
          _lastScreenEvent = event;
          _recentScreenEvents.insert(
            0,
            _ScreenEventEntry(event: event, timestamp: DateTime.now()),
          );
          if (_recentScreenEvents.length > _maxRecentScreenEvents) {
            _recentScreenEvents.removeRange(
              _maxRecentScreenEvents,
              _recentScreenEvents.length,
            );
          }
        });
      },
      onError: (Object error) {
        log(
          'Screen event error: $error',
          name: 'ScreenLockPluginExample',
          error: error,
        );
      },
    );
  }

  void _showRecentScreenEventsDialog() {
    showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Last 10 screen events'),
          content: SizedBox(
            width: double.maxFinite,
            child: _recentScreenEvents.isEmpty
                ? const Text('No screen events recorded yet.')
                : ListView.separated(
                    shrinkWrap: true,
                    itemCount: _recentScreenEvents.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (_, index) {
                      final entry = _recentScreenEvents[index];
                      return ListTile(
                        dense: true,
                        leading: CircleAvatar(
                          radius: 14,
                          child: Text('${index + 1}'),
                        ),
                        title: Text(entry.event),
                        subtitle: Text(entry.formattedTimestamp),
                      );
                    },
                  ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('Close'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _requestAdmin() async {
    log('Requesting device admin', name: 'ScreenLockPluginExample');
    await _screenLockPlugin.requestDeviceAdmin();
    await _checkAdminStatus();
  }

  Future<void> _lockScreen() async {
    log('Attempting to lock screen', name: 'ScreenLockPluginExample');
    final result = await _screenLockPlugin.lockScreen();
    log('Lock screen result: $result', name: 'ScreenLockPluginExample');
    if (result != true) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Device admin not enabled. Please enable it first.'),
          ),
        );
      }
    }
  }

  @override
  void dispose() {
    log('Disposing screen event subscription', name: 'ScreenLockPluginExample');
    _screenEventSub?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Screen Lock Plugin Example')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Device Admin Status: ${_isAdminEnabled ? "Enabled" : "Disabled"}',
                style: const TextStyle(fontSize: 18),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              Text(
                'Screen is currently: ${_isScreenOn == null ? "..." : (_isScreenOn! ? "ON" : "OFF")}',
                style: const TextStyle(fontSize: 18),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              InkWell(
                onTap: _showRecentScreenEventsDialog,
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Text(
                    'Last screen event: $_lastScreenEvent',
                    style: const TextStyle(
                      fontSize: 16,
                      decoration: TextDecoration.underline,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Notifications permission: ${_notificationStatus.name}',
                style: const TextStyle(fontSize: 14),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: _isAdminEnabled ? null : _requestAdmin,
                child: const Text('Enable Device Admin'),
              ),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: _isAdminEnabled ? _lockScreen : null,
                child: const Text('Lock Screen'),
              ),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: _checkScreenState,
                child: const Text('Check Screen State'),
              ),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: _checkAdminStatus,
                child: const Text('Refresh Status'),
              ),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: _requestNotificationPermission,
                child: const Text('Request Notification Permission'),
              ),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: _refreshPermissionStatuses,
                child: const Text('Refresh Permission Status'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ScreenEventEntry {
  _ScreenEventEntry({required this.event, required this.timestamp});

  final String event;
  final DateTime timestamp;

  String get formattedTimestamp {
    final local = timestamp.toLocal();
    String two(int value) => value.toString().padLeft(2, '0');
    return '${two(local.hour)}:${two(local.minute)}:${two(local.second)}';
  }
}

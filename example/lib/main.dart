import 'package:flutter/material.dart';
import 'package:screen_lock_plugin/screen_lock_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _screenLockPlugin = ScreenLockPlugin();
  bool _isAdminEnabled = false;

  @override
  void initState() {
    super.initState();
    _checkAdminStatus();
  }

  Future<void> _checkAdminStatus() async {
    final isEnabled = await _screenLockPlugin.isDeviceAdminEnabled();
    setState(() {
      _isAdminEnabled = isEnabled ?? false;
    });
  }

  Future<void> _requestAdmin() async {
    await _screenLockPlugin.requestDeviceAdmin();
    await _checkAdminStatus();
  }

  Future<void> _lockScreen() async {
    final result = await _screenLockPlugin.lockScreen();
    if (!result!) {
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
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Screen Lock Plugin Example')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'Device Admin Status: ${_isAdminEnabled ? "Enabled" : "Disabled"}',
                style: const TextStyle(fontSize: 18),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: _isAdminEnabled ? null : _requestAdmin,
                child: const Text('Enable Device Admin'),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: _isAdminEnabled ? _lockScreen : null,
                child: const Text('Lock Screen'),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: _checkAdminStatus,
                child: const Text('Refresh Status'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

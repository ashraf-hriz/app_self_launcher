import 'package:app_self_launcher/app_self_launcher.dart';
import 'package:flutter/material.dart';

void main() => runApp(const ExampleApp());

class ExampleApp extends StatelessWidget {
  const ExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: LauncherExamplePage(),
    );
  }
}

class LauncherExamplePage extends StatefulWidget {
  const LauncherExamplePage({super.key});

  @override
  State<LauncherExamplePage> createState() => _LauncherExamplePageState();
}

class _LauncherExamplePageState extends State<LauncherExamplePage>
    with WidgetsBindingObserver {
  bool? _overlayGranted;
  bool? _launchDispatched;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refreshOverlayPermission();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _refreshOverlayPermission();
    }
  }

  Future<void> _refreshOverlayPermission() async {
    final granted = await AppSelfLauncher.isOverlayPermissionGranted();
    if (mounted) setState(() => _overlayGranted = granted);
  }

  Future<void> _requestOverlayPermission() async {
    await AppSelfLauncher.requestOverlayPermission();
  }

  Future<void> _launchApp() async {
    final dispatched = await AppSelfLauncher.launchApp(
      extras: const <String, Object?>{'source': 'example'},
    );
    if (mounted) setState(() => _launchDispatched = dispatched);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('App Self Launcher')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Text('Overlay permission: ${_overlayGranted ?? 'checking'}'),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: _requestOverlayPermission,
              child: const Text('Open overlay settings'),
            ),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: _launchApp,
              child: const Text('Launch this app'),
            ),
            if (_launchDispatched != null)
              Text('Launch dispatched: $_launchDispatched'),
          ],
        ),
      ),
    );
  }
}

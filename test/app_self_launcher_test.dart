import 'package:app_self_launcher/app_self_launcher.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('app_self_launcher');
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  tearDown(() {
    debugDefaultTargetPlatformOverride = null;
    messenger.setMockMethodCallHandler(channel, null);
  });

  test('non-Android platforms use safe fallback values', () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    var channelCalls = 0;
    messenger.setMockMethodCallHandler(channel, (call) async {
      channelCalls++;
      return null;
    });

    expect(await AppSelfLauncher.launchApp(), isFalse);
    expect(await AppSelfLauncher.isOverlayPermissionGranted(), isTrue);
    expect(await AppSelfLauncher.requestOverlayPermission(), isTrue);
    expect(channelCalls, 0);
  });

  test('launch forwards extras and returns the native result', () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    MethodCall? receivedCall;
    messenger.setMockMethodCallHandler(channel, (call) async {
      receivedCall = call;
      return true;
    });

    final result = await AppSelfLauncher.launchApp(
      extras: const <String, Object?>{
        'enabled': true,
        'count': 2,
        'ratio': 1.5,
        'message': 'ready',
        'empty': null,
      },
    );

    expect(result, isTrue);
    expect(receivedCall?.method, 'launchApp');
    expect(
      receivedCall?.arguments,
      <String, Object?>{
        'extras': <String, Object?>{
          'enabled': true,
          'count': 2,
          'ratio': 1.5,
          'message': 'ready',
          'empty': null,
        },
      },
    );
  });

  test('launch returns false when native launch fails', () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    messenger.setMockMethodCallHandler(channel, (call) async => false);

    expect(await AppSelfLauncher.launchApp(), isFalse);
  });

  test('granted overlay permission does not open settings', () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    var openSettingsCalls = 0;
    messenger.setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'canDrawOverlays') return true;
      if (call.method == 'openOverlaySettings') openSettingsCalls++;
      return null;
    });

    expect(await AppSelfLauncher.requestOverlayPermission(), isTrue);
    expect(openSettingsCalls, 0);
  });

  test('denied overlay permission opens settings and remains denied', () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    MethodCall? settingsCall;
    messenger.setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'canDrawOverlays') return false;
      if (call.method == 'openOverlaySettings') {
        settingsCall = call;
        return true;
      }
      return null;
    });

    const prompt = OverlayPermissionPrompt(
      title: 'Title',
      message: 'Message',
      positiveButtonLabel: 'Continue',
    );
    expect(
      await AppSelfLauncher.requestOverlayPermission(prompt: prompt),
      isFalse,
    );
    expect(settingsCall?.method, 'openOverlaySettings');
    expect(settingsCall?.arguments, prompt.toMap());
  });

  test('channel failures return safe failure values', () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;
    messenger.setMockMethodCallHandler(channel, (call) async {
      throw PlatformException(code: 'native-error');
    });

    expect(await AppSelfLauncher.launchApp(), isFalse);
    expect(await AppSelfLauncher.isOverlayPermissionGranted(), isFalse);
    expect(await AppSelfLauncher.requestOverlayPermission(), isFalse);
  });

  test('missing plugin returns safe failure values', () async {
    debugDefaultTargetPlatformOverride = TargetPlatform.android;

    expect(await AppSelfLauncher.launchApp(), isFalse);
    expect(await AppSelfLauncher.isOverlayPermissionGranted(), isFalse);
    expect(await AppSelfLauncher.requestOverlayPermission(), isFalse);
  });
}

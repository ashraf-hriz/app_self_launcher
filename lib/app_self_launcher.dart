library app_self_launcher;

import 'dart:developer' as developer;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Text shown before opening Xiaomi's app-specific permission editor.
@immutable
final class OverlayPermissionPrompt {
  const OverlayPermissionPrompt({
    this.title = 'Additional Permission Required',
    this.message =
        'Enable necessary system permissions to receive notifications.',
    this.positiveButtonLabel = 'Open Settings',
  });

  final String title;
  final String message;
  final String positiveButtonLabel;

  Map<String, String> toMap() => <String, String>{
        'title': title,
        'message': message,
        'positiveButtonLabel': positiveButtonLabel,
      };
}

/// Launches the consuming application and manages its Android overlay setting.
abstract final class AppSelfLauncher {
  static const MethodChannel _channel = MethodChannel('app_self_launcher');

  static bool get _isAndroid =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  /// Asks Android to bring the consuming application's launcher activity to
  /// the foreground.
  ///
  /// A `true` result means Android accepted the start request. Android can
  /// still prevent the activity from becoming visible because of background
  /// activity launch restrictions.
  static Future<bool> launchApp({
    Map<String, Object?> extras = const <String, Object?>{},
  }) async {
    if (!_isAndroid) return false;

    try {
      return await _channel.invokeMethod<bool>(
            'launchApp',
            <String, Object?>{'extras': extras},
          ) ??
          false;
    } on PlatformException catch (error, stackTrace) {
      _logFailure('Unable to launch the host app', error.code, stackTrace);
      return false;
    } on MissingPluginException catch (_, stackTrace) {
      _logFailure('App launcher channel is unavailable', null, stackTrace);
      return false;
    }
  }

  /// Returns `true` when overlay permission is granted or Android does not
  /// expose an overlay permission settings screen for this app.
  static Future<bool> isOverlayPermissionGranted() async {
    if (!_isAndroid) return true;

    try {
      return await _channel.invokeMethod<bool>('canDrawOverlays') ?? false;
    } on PlatformException catch (error, stackTrace) {
      _logFailure(
        'Unable to check overlay permission',
        error.code,
        stackTrace,
      );
      return false;
    } on MissingPluginException catch (_, stackTrace) {
      _logFailure(
          'Overlay permission channel is unavailable', null, stackTrace);
      return false;
    }
  }

  /// Opens Android's overlay settings when permission is required.
  ///
  /// Returns `true` only if permission was already granted (or no settings
  /// screen exists). Opening settings returns `false`; callers must check again
  /// after their app resumes.
  static Future<bool> requestOverlayPermission({
    OverlayPermissionPrompt prompt = const OverlayPermissionPrompt(),
  }) async {
    if (!_isAndroid) return true;
    if (await isOverlayPermissionGranted()) return true;

    try {
      await _channel.invokeMethod<bool>('openOverlaySettings', prompt.toMap());
    } on PlatformException catch (error, stackTrace) {
      _logFailure(
        'Unable to open overlay permission settings',
        error.code,
        stackTrace,
      );
    } on MissingPluginException catch (_, stackTrace) {
      _logFailure(
          'Overlay permission channel is unavailable', null, stackTrace);
    }

    return false;
  }

  static void _logFailure(String message, String? code, StackTrace stackTrace) {
    developer.log(
      code == null ? message : '$message (code: $code)',
      name: 'app_self_launcher',
      stackTrace: stackTrace,
    );
  }
}

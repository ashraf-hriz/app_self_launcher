# app_self_launcher

An Android Flutter plugin that asks the operating system to launch the
consuming app and opens its **Display over other apps** settings when needed.
Your application keeps ownership of notification handling, schedules, and all
other business rules that decide when a launch should be requested.

## Platform behavior

| API | Android | Other platforms |
| --- | --- | --- |
| `launchApp()` | Dispatches the host app's launcher intent | Returns `false` |
| `isOverlayPermissionGranted()` | Checks permission or settings availability | Returns `true` |
| `requestOverlayPermission()` | Opens settings when needed | Returns `true` |

Android restricts background activity launches. A `true` value from
`launchApp()` means the start request was dispatched; it cannot guarantee that
Android displayed the activity. Review the
[Android background activity launch documentation](https://developer.android.com/guide/components/activities/background-starts)
before using this behavior.

## Installation

```yaml
dependencies:
  app_self_launcher: ^0.1.0
```

The plugin adds `android.permission.SYSTEM_ALERT_WINDOW` to the merged Android
manifest. Google Play applies special policies to apps that request this
permission, so use it only when your app's core behavior requires it.

## Launch the host app

```dart
import 'package:app_self_launcher/app_self_launcher.dart';

final dispatched = await AppSelfLauncher.launchApp(
  extras: <String, Object?>{
    'notification': true,
    'message_id': '42',
  },
);
```

Extra values may be `null`, `bool`, `int`, `double`, or `String`. The package
resolves the launcher activity from the consuming app's package name, so no
application ID or Android activity class is configured in Dart.

## Request overlay permission

```dart
final isGranted = await AppSelfLauncher.isOverlayPermissionGranted();
if (!isGranted) {
  await AppSelfLauncher.requestOverlayPermission();
}
```

Opening settings is not proof that permission was granted. When settings are
opened, `requestOverlayPermission()` returns `false`. Check again when the app
resumes:

```dart
@override
void didChangeAppLifecycleState(AppLifecycleState state) {
  if (state == AppLifecycleState.resumed) {
    AppSelfLauncher.isOverlayPermissionGranted();
  }
}
```

On Xiaomi devices, the plugin shows a short explanation before opening MIUI's
permission editor when a Flutter activity is attached. Customize that text per
request when needed:

```dart
await AppSelfLauncher.requestOverlayPermission(
  prompt: const OverlayPermissionPrompt(
    title: 'Permission required',
    message: 'Enable the additional permission for incoming job alerts.',
    positiveButtonLabel: 'Open settings',
  ),
);
```

When called from a background Flutter engine without an attached activity, the
plugin can still launch the host application. Overlay settings are opened
directly because an Android dialog requires an activity.

## License

MIT

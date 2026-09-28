package com.ashrafhriz.app_self_launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.Locale

class AppSelfLauncherPlugin :
    FlutterPlugin,
    MethodChannel.MethodCallHandler,
    ActivityAware {
    private lateinit var applicationContext: Context
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "launchApp" -> result.success(launchApp(call))
            "canDrawOverlays" -> result.success(canDrawOverlaysCompat())
            "openOverlaySettings" -> result.success(openOverlaySettings(call))
            else -> result.notImplemented()
        }
    }

    private fun launchApp(call: MethodCall): Boolean {
        val arguments = call.arguments as? Map<*, *>
        val extras = arguments?.get("extras") as? Map<*, *> ?: emptyMap<Any, Any?>()
        return AppLaunchDispatcher(applicationContext).launch(extras)
    }

    private fun canDrawOverlaysCompat(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        if (Settings.canDrawOverlays(applicationContext)) return true
        return !hasOverlaySettingsActivity()
    }

    private fun hasOverlaySettingsActivity(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        if (createStandardOverlayIntent().resolveActivity(
                applicationContext.packageManager
            ) != null
        ) {
            return true
        }
        return createXiaomiPermissionIntent()?.resolveActivity(
            applicationContext.packageManager
        ) != null
    }

    private fun openOverlaySettings(call: MethodCall): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(applicationContext)
        ) {
            return true
        }

        val xiaomiIntent = createXiaomiPermissionIntent()?.takeIf {
            it.resolveActivity(applicationContext.packageManager) != null
        }
        if (xiaomiIntent != null) {
            return openXiaomiSettings(call, xiaomiIntent)
        }

        val standardIntent = createStandardOverlayIntent()
        if (standardIntent.resolveActivity(applicationContext.packageManager) == null) {
            return false
        }
        startSettingsActivity(standardIntent)
        return true
    }

    private fun openXiaomiSettings(call: MethodCall, intent: Intent): Boolean {
        val currentActivity = activity
        if (currentActivity == null || currentActivity.isFinishing) {
            startSettingsActivity(intent)
            return true
        }

        val title = call.argument<String>("title") ?: DEFAULT_PROMPT_TITLE
        val message = call.argument<String>("message") ?: DEFAULT_PROMPT_MESSAGE
        val positiveLabel = call.argument<String>("positiveButtonLabel")
            ?: DEFAULT_PROMPT_POSITIVE_LABEL

        AlertDialog.Builder(currentActivity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveLabel) { _, _ -> currentActivity.startActivity(intent) }
            .setCancelable(false)
            .show()
        return true
    }

    private fun startSettingsActivity(intent: Intent) {
        val currentActivity = activity
        if (currentActivity != null && !currentActivity.isFinishing) {
            currentActivity.startActivity(intent)
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(intent)
    }

    private fun createStandardOverlayIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${applicationContext.packageName}")
    )

    private fun createXiaomiPermissionIntent(): Intent? {
        if (Build.MANUFACTURER.lowercase(Locale.ROOT) != "xiaomi") return null
        return Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            putExtra("extra_pkgname", applicationContext.packageName)
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        activity = null
    }

    private companion object {
        const val CHANNEL_NAME = "app_self_launcher"
        const val DEFAULT_PROMPT_TITLE = "Additional Permission Required"
        const val DEFAULT_PROMPT_MESSAGE =
            "Enable necessary system permissions to receive notifications."
        const val DEFAULT_PROMPT_POSITIVE_LABEL = "Open Settings"
    }
}

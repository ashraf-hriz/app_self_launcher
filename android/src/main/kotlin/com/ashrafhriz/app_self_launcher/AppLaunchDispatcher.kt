package com.ashrafhriz.app_self_launcher

import android.content.Context
import android.content.Intent
import android.util.Log

internal class AppLaunchDispatcher(private val context: Context) {
    fun launch(extras: Map<*, *>): Boolean {
        return try {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?: return false.also {
                    Log.w(TAG, "No launcher activity exists for the host application")
                }

            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            extras.forEach { (key, value) ->
                require(key is String) { "Every extra key must be a String" }
                putPrimitiveExtra(launchIntent, key, value)
            }

            context.startActivity(launchIntent)
            true
        } catch (error: Exception) {
            Log.e(TAG, "Unable to dispatch the host application launch", error)
            false
        }
    }

    private fun putPrimitiveExtra(intent: Intent, key: String, value: Any?) {
        when (value) {
            null -> intent.putExtra(key, null as String?)
            is Boolean -> intent.putExtra(key, value)
            is Byte -> intent.putExtra(key, value)
            is Char -> intent.putExtra(key, value)
            is Double -> intent.putExtra(key, value)
            is Float -> intent.putExtra(key, value)
            is Int -> intent.putExtra(key, value)
            is Long -> intent.putExtra(key, value)
            is Short -> intent.putExtra(key, value)
            is String -> intent.putExtra(key, value)
            else -> throw IllegalArgumentException(
                "Extra '$key' must contain a primitive value or String"
            )
        }
    }

    private companion object {
        const val TAG = "AppSelfLauncher"
    }
}

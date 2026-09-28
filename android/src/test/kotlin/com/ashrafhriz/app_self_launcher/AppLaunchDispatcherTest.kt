package com.ashrafhriz.app_self_launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppLaunchDispatcherTest {
    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        packageManager = mock(PackageManager::class.java)
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(context.packageName).thenReturn(HOST_PACKAGE)
    }

    @Test
    fun launch_resolvesHostActivity_forwardsExtrasAndFlags_withoutActivity() {
        `when`(packageManager.getLaunchIntentForPackage(HOST_PACKAGE))
            .thenReturn(Intent(Intent.ACTION_MAIN))

        val result = AppLaunchDispatcher(context).launch(
            mapOf(
                "enabled" to true,
                "count" to 4,
                "ratio" to 1.5,
                "message" to "ready",
                "empty" to null,
            )
        )

        assertTrue(result)
        val captor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivity(captor.capture())
        val launchedIntent = captor.value
        assertTrue(launchedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(launchedIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(launchedIntent.getBooleanExtra("enabled", false))
        assertEquals(4, launchedIntent.getIntExtra("count", 0))
        assertEquals(1.5, launchedIntent.getDoubleExtra("ratio", 0.0), 0.0)
        assertEquals("ready", launchedIntent.getStringExtra("message"))
        assertTrue(launchedIntent.hasExtra("empty"))
    }

    @Test
    fun launch_returnsFalse_whenHostHasNoLauncherActivity() {
        `when`(packageManager.getLaunchIntentForPackage(HOST_PACKAGE)).thenReturn(null)

        assertFalse(AppLaunchDispatcher(context).launch(emptyMap<String, Any?>()))
    }

    @Test
    fun launch_returnsFalse_whenExtraIsNotPrimitive() {
        `when`(packageManager.getLaunchIntentForPackage(HOST_PACKAGE))
            .thenReturn(Intent(Intent.ACTION_MAIN))

        assertFalse(AppLaunchDispatcher(context).launch(mapOf("items" to listOf(1))))
    }

    private companion object {
        const val HOST_PACKAGE = "dev.example.host"
    }
}

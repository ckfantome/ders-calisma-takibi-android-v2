package com.derscalismatakibi.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derscalismatakibi.app.util.AccessibilityHelper
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug 2: reboot sonrasi ENABLED_ACCESSIBILITY_SERVICES "etkin" gorunse bile
 * servis gercekte baglanmamis olabiliyordu. AccessibilityHelper.recordHeartbeat/
 * lastHeartbeat() bu canliligi disariya (StudyEngine'in bekci dongusune)
 * yansitan mekanizma - burada round-trip'i dogruluyoruz.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityHeartbeatInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun heartbeatReflectsRecentWrite() {
        val before = System.currentTimeMillis()
        AccessibilityHelper.recordHeartbeat(context)
        val after = System.currentTimeMillis()

        val recorded = AccessibilityHelper.lastHeartbeat(context)
        assertTrue("heartbeat should be within the write window", recorded in before..after)
    }

    @Test
    fun heartbeatIsMonotonic() {
        AccessibilityHelper.recordHeartbeat(context)
        val first = AccessibilityHelper.lastHeartbeat(context)
        Thread.sleep(10)
        AccessibilityHelper.recordHeartbeat(context)
        val second = AccessibilityHelper.lastHeartbeat(context)
        assertTrue("second heartbeat must not be older than the first", second >= first)
    }
}

package com.derscalismatakibi.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.derscalismatakibi.app.BlockedActivity
import com.derscalismatakibi.app.core.BlockReason
import com.derscalismatakibi.app.core.StudyEngine
import com.derscalismatakibi.app.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Uygulama Kilidi: on plandaki uygulama degisikligini yakalar, engelliyse
 * BlockedActivity'yi on plana getirir. Icerik OKUMAZ (accessibility_service_config.xml
 * -> canRetrieveWindowContent=false), sadece hangi paketin acildigini gorur. */
class AppBlockAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == applicationContext.packageName) return
        scope.launch {
            StudyEngine.init(applicationContext)
            val reason = StudyEngine.isPackageBlocked(pkg) ?: return@launch
            AppLogger.log("UygulamaKilidi", "$pkg engellendi: $reason")
            if (reason == BlockReason.ExamMode) {
                val label = try {
                    packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
                } catch (_: Exception) {
                    pkg
                }
                StudyEngine.sendInstantAlertEmail(
                    "Sinav Modunda Engellenen Uygulama",
                    "$label ($pkg) sinav/odev modu acikken acilmaya calisildi.",
                )
            }
            startActivity(
                Intent(applicationContext, BlockedActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(BlockedActivity.EXTRA_REASON, BlockedActivity.reasonExtra(reason))
                },
            )
        }
    }

    override fun onInterrupt() {}
}

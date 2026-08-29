package com.derscalismatakibi.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.derscalismatakibi.app.BlockedActivity
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.core.BlockReason
import com.derscalismatakibi.app.core.StudyEngine
import com.derscalismatakibi.app.data.AppDatabase
import com.derscalismatakibi.app.data.KeystrokeLogEntity
import com.derscalismatakibi.app.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Uygulama Kilidi: on plandaki uygulama degisikligini yakalar, engelliyse
 * BlockedActivity'yi on plana getirir. Klavye Takibi (AppConfig.keyboardTrackingEnabled,
 * varsayilan KAPALI) acikken yazilan metinleri de kaydeder - sifre alanlari HARIC. */
class AppBlockAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowChange(event)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleTextChanged(event)
        }
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == applicationContext.packageName) return
        scope.launch {
            StudyEngine.init(applicationContext)
            val reason = StudyEngine.isPackageBlocked(pkg) ?: return@launch
            AppLogger.log("UygulamaKilidi", "$pkg engellendi: $reason")
            if (reason == BlockReason.ExamMode) {
                val label = appLabel(pkg)
                StudyEngine.sendInstantAlertEmail(
                    applicationContext.getString(R.string.app_block_email_subject),
                    applicationContext.getString(R.string.app_block_email_body, label, pkg),
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

    private fun handleTextChanged(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == applicationContext.packageName) return
        // Sifre alanlari KESINLIKLE kaydedilmez - AccessibilityNodeInfo.isPassword
        // hem parola inputType'li TextView'lerde hem de sistem klavyesindeki
        // gizli-karakter alanlarinda dogru sekilde true doner. NOT: buraya yazilan
        // metnin AppLogger'a (Loglar ekraninda Paylas ile disari cikan genel log)
        // ASLA yazilmamasi gerekir - sadece asagidaki ozel/yonetici-gorebilir
        // KeystrokeLogEntity tablosuna gider.
        if (event.source?.isPassword == true) return
        val text = event.text?.joinToString(" ")?.trim().orEmpty()
        if (text.isEmpty()) return
        scope.launch {
            StudyEngine.init(applicationContext)
            if (!StudyEngine.currentConfig().keyboardTrackingEnabled) return@launch
            try {
                val dao = AppDatabase.getInstance(applicationContext).keystrokeLogDao()
                dao.insert(
                    KeystrokeLogEntity(
                        packageName = pkg,
                        appLabel = appLabel(pkg),
                        text = text.take(500),
                        timestamp = System.currentTimeMillis(),
                    ),
                )
                dao.trimToRecent()
            } catch (t: Throwable) {
                AppLogger.logError("KlavyeTakibi", "Kayit yazilamadi", t)
            }
        }
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        pkg
    }

    override fun onInterrupt() {}
}

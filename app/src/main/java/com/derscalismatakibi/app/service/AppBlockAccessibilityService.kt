package com.derscalismatakibi.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import com.derscalismatakibi.app.BlockedActivity
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.core.BlockReason
import com.derscalismatakibi.app.core.StudyEngine
import com.derscalismatakibi.app.data.AppDatabase
import com.derscalismatakibi.app.data.KeystrokeLogEntity
import com.derscalismatakibi.app.util.AccessibilityHelper
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

    companion object {
        private var cachedImePackages: Set<String> = emptySet()
        private var imePackagesRefreshedAt: Long = 0L
        private const val IME_PACKAGES_TTL_MS = 5 * 60 * 1000L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityHelper.recordHeartbeat(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // OS'in servisi "etkin" gostermesi gercekten calistigi anlamina gelmez
        // (reboot sonrasi bazi OEM'lerde servis yeniden baglanmaz) - her event
        // burada bir "hala canliyim" damgasi birakir, StudyEngine'in bekcisi bunu
        // ENABLED_ACCESSIBILITY_SERVICES kontrolune ek olarak kullanir.
        AccessibilityHelper.recordHeartbeat(applicationContext)
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowChange(event)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> handleTextChanged(event)
        }
    }

    /** TYPE_WINDOW_STATE_CHANGED sadece genuine uygulama gecislerinde degil, dialog/
     * sistem overlay/klavye pencereleri on plana geldiginde de tetiklenir. Event'in
     * kaynak node'unun penceresini kontrol ederek gercek bir uygulama penceresi mi
     * (TYPE_APPLICATION) yoksa sistem/klavye/overlay penceresi mi oldugunu ayirt eder.
     * event.source null ise (recycled node, bazi OEM'lerde olur) tip belirlenemez -> null. */
    private fun windowType(event: AccessibilityEvent): Int? = try {
        event.source?.window?.type
    } catch (_: Throwable) {
        null
    }

    private fun enabledImePackages(): Set<String> {
        val now = System.currentTimeMillis()
        if (now - imePackagesRefreshedAt > IME_PACKAGES_TTL_MS) {
            imePackagesRefreshedAt = now
            cachedImePackages = try {
                (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.enabledInputMethodList?.map { it.packageName }?.toSet() ?: cachedImePackages
            } catch (_: Throwable) {
                cachedImePackages
            }
        }
        return cachedImePackages
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: event.source?.packageName?.toString() ?: return
        if (pkg == applicationContext.packageName) return
        // Pencere tipi bilinip de uygulama penceresi degilse (sistem/klavye/overlay),
        // bu gercek bir "uygulama degisti" olayi degildir - erken cik. Tip
        // belirlenemiyorsa (null) eski davranisa (paket adi tabanli) devam edilir.
        val type = windowType(event)
        if (type != null && type != AccessibilityWindowInfo.TYPE_APPLICATION) return
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
        // Ozellik varsayilan KAPALI - typeWindowContentChanged cok sik tetiklendigi
        // icin, kapaliyken asagidaki pencere-tipi/IME/sifre/metin islemlerine hic
        // girmeden en basta cikilir. cfg, StudyEngine.init() cagrilmadan once bile
        // AppConfig() varsayilanina sahip (bkz. StudyEngine.kt cfg tanimi) - bu
        // yuzden burada init() cagirmadan (main thread'de agir islem yapmadan)
        // dogrudan okunabilir; asil init() asagida arka plan coroutine'inde olur.
        if (!StudyEngine.currentConfig().keyboardTrackingEnabled) return
        // Timestamp event alinir alinmaz, senkron olarak damgalanir - asagidaki
        // coroutine'ler sirasiz tamamlanabildigi icin DB'ye yazilan zaman event'in
        // GERCEK alinma sirasini yansitir, coroutine'in calisma sirasini degil.
        val timestamp = System.currentTimeMillis()
        val pkg = event.packageName?.toString() ?: event.source?.packageName?.toString() ?: return
        if (pkg == applicationContext.packageName) return
        // Klavyenin KENDI penceresi (arama/pano/emoji kutusu vb.) icinde yazilan
        // metin, "pkg altinda kullanici yazi yazdi" demek degildir - klavye
        // penceresi (TYPE_INPUT_METHOD) ya da bilinen bir IME paketi ise atla.
        // Hangi klavyenin kurulu/etkin oldugu hardcode edilmez, InputMethodManager'dan
        // okunur (bkz. enabledImePackages).
        if (windowType(event) == AccessibilityWindowInfo.TYPE_INPUT_METHOD) return
        if (pkg in enabledImePackages()) return
        // Sifre alanlari KESINLIKLE kaydedilmez - AccessibilityNodeInfo.isPassword
        // hem parola inputType'li TextView'lerde hem de sistem klavyesindeki
        // gizli-karakter alanlarinda dogru sekilde true doner. event.source null
        // olup bayrak OKUNAMIYORSA da guvenli taraf secilir (sifre VARSAYILIR,
        // kaydedilmez) - sadece isPassword kesin false donunce kaydedilir. NOT:
        // buraya yazilan metnin AppLogger'a (Loglar ekraninda Paylas ile disari
        // cikan genel log) ASLA yazilmamasi gerekir - sadece asagidaki ozel/
        // yonetici-gorebilir KeystrokeLogEntity tablosuna gider.
        if (event.source?.isPassword != false) return
        // TYPE_VIEW_TEXT_CHANGED genelde event.text'i doldurur; TYPE_VIEW_FOCUSED /
        // TYPE_WINDOW_CONTENT_CHANGED (Compose/WebView alanlarini yakalamak icin
        // eklendi) coguzaman event.text'i bos birakir, gercek metin node'un
        // (event.source) uzerindedir - bu yuzden ikisi de denenir.
        val text = event.text?.joinToString(" ")?.trim()?.takeIf { it.isNotEmpty() }
            ?: event.source?.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        scope.launch {
            StudyEngine.init(applicationContext)
            try {
                val dao = AppDatabase.getInstance(applicationContext).keystrokeLogDao()
                dao.insert(
                    KeystrokeLogEntity(
                        packageName = pkg,
                        appLabel = appLabel(pkg),
                        text = text.take(500),
                        timestamp = timestamp,
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

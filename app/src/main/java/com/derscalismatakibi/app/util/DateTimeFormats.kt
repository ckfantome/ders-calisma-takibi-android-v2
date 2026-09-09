package com.derscalismatakibi.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Merkezi tarih/saat gosterim bicimleri - loglar ve zaman damgasi gosteren
 * ekranlar/dosyalar BURADAN kullanmali, kendi SimpleDateFormat'ini
 * olusturmamali. Amac: projede dagilmis/tutarsiz (bazen sadece saat, tarihsiz)
 * formatlarin onune gecip her yerde ayni gun/ay/yil/saat/dakika/saniye
 * bicimini kullanmak. Her cagri kendi SimpleDateFormat ornegini olusturur
 * (SimpleDateFormat thread-safe DEGIL, paylasilan bir alan arka plan
 * thread'lerinden (Servis/AccessibilityService) ayni anda cagrilirsa bozuk
 * sonuc uretebilir) - mevcut kod tabanindaki diger tum kullanimlarla ayni
 * guvenli desen.
 */
object DateTimeFormats {
    /** Ayarlar > Dil (AppConfig.appLanguage) ile uyumlu Locale - StudyEngine
     * henuz baslatilmamissa (cok erken cagri) varsayilan Turkce'ye duser. */
    private fun activeLocale(): Locale =
        if (runCatching { com.derscalismatakibi.app.core.StudyEngine.currentConfig().appLanguage }.getOrDefault("tr") == "en") {
            Locale.US
        } else {
            Locale("tr")
        }

    /** Tam zaman damgasi: gun.ay.yil saat:dakika:saniye.milisaniye - log
     * satirlari icin (AppLogger). */
    fun logTimestamp(timestamp: Long): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS", activeLocale()).format(Date(timestamp))

    /** Tam zaman damgasi, milisaniyesiz - kullanici arayuzunde listelenen
     * zaman damgalari icin (Kullanim/Klavye/Arama loglari vb). */
    fun display(timestamp: Long): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss", activeLocale()).format(Date(timestamp))
}

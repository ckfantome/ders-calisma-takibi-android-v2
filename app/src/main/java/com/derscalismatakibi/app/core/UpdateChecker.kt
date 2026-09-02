package com.derscalismatakibi.app.core

import android.util.Log
import com.derscalismatakibi.app.util.AppLogger
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Releases uzerinden guncelleme kontrolu - study_tracker2.py'de karsiligi
 * yok (masaustu icin gerek yok). Uygulama Play Store'a degil GitHub'a sideload
 * dagitildigi icin kendi guncelleme kontrolunu kendi yapiyor. Repo PUBLIC oldugu
 * icin kimlik dogrulama gerekmez.
 */
object UpdateChecker {
    // ders-calisma-takibi-android artik PRIVATE (v0.41.0 oncesi yapilan repo
    // tasima) - API 404 donup guncelleme kontrolu sessizce hicbir sey
    // bulamiyordu, indirme hic tetiklenmiyordu. Yeni depoya guncellendi.
    private const val REPO = "ckfantome/ders-calisma-takibi-android-v2"
    private const val API_URL = "https://api.github.com/repos/$REPO/releases/latest"

    data class UpdateInfo(
        val version: String,
        val notes: String,
        val downloadUrl: String,
        val assetName: String,
    )

    /**
     * Kontrol sonucu: "guncel" ile "kontrol basarisiz" (rate limit, agsizlik, vb.)
     * ayrimini kullaniciya gosterebilmek icin ayri durumlar tutulur - bu ikisi
     * UI'da ayni "guncelsiniz" mesajina donusursa gercek bir hata sessizce
     * yutulmus olur.
     */
    sealed class CheckResult {
        data class Available(val info: UpdateInfo) : CheckResult()
        object UpToDate : CheckResult()
        data class Failed(val reason: String) : CheckResult()
    }

    /**
     * AG thread'inde (network) calisir - cagiran taraf coroutine icinde
     * Dispatchers.IO ile sarmalamali.
     */
    fun checkForUpdateDetailed(currentVersionName: String): CheckResult {
        return try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }
                } catch (e: Exception) {
                    null
                }
                conn.disconnect()
                val reason = "HTTP $code" + (errorBody?.let { JSONObject(it).optString("message", "") }?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "")
                Log.w("UpdateChecker", "Guncelleme kontrolu basarisiz: $reason")
                AppLogger.logError("Guncelleme", "Kontrol basarisiz ($reason)", null)
                return CheckResult.Failed(reason)
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            val version = tag.removePrefix("v")
            if (version.isBlank() || !isNewer(version, currentVersionName)) {
                AppLogger.log("Guncelleme", "Kontrol edildi - en son surumde (mevcut: $currentVersionName, uzak: $version)")
                return CheckResult.UpToDate
            }
            AppLogger.log("Guncelleme", "Yeni surum bulundu: $version (mevcut: $currentVersionName)")

            val assets = json.optJSONArray("assets")
                ?: return CheckResult.Failed("release'de asset bulunamadi")
            var url: String? = null
            var name: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val n = a.optString("name")
                if (n.endsWith(".apk")) {
                    url = a.optString("browser_download_url")
                    name = n
                    break
                }
            }
            val downloadUrl = url ?: return CheckResult.Failed("release'de APK asset'i bulunamadi")
            CheckResult.Available(
                UpdateInfo(
                    version = version,
                    notes = json.optString("body", ""),
                    downloadUrl = downloadUrl,
                    assetName = name ?: "update.apk",
                ),
            )
        } catch (e: Exception) {
            Log.w("UpdateChecker", "Guncelleme kontrolu basarisiz: ${e.message}")
            AppLogger.logError("Guncelleme", "Kontrol basarisiz", e)
            CheckResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Geriye donuk uyumluluk icin: hem "guncel" hem "kontrol basarisiz"
     * durumlarinda sessizce null doner (otomatik acilis kontrolu icin -
     * kullaniciyi network hatasiyla rahatsiz etmeye gerek yok). Kullaniciya
     * hata gosterilmesi gereken yerlerde (manuel "guncellemeleri kontrol et"
     * butonu gibi) bunun yerine [checkForUpdateDetailed] kullanilmali.
     */
    fun checkForUpdate(currentVersionName: String): UpdateInfo? =
        (checkForUpdateDetailed(currentVersionName) as? CheckResult.Available)?.info

    /** Basit semver karsilastirmasi: "0.4.0" > "0.3.0" -> true. */
    internal fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(r.size, c.size)
        for (i in 0 until len) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }
}

package com.derscalismatakibi.app.core

import android.util.Log
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
    private const val REPO = "ckfantome/ders-calisma-takibi-android"
    private const val API_URL = "https://api.github.com/repos/$REPO/releases/latest"

    data class UpdateInfo(
        val version: String,
        val notes: String,
        val downloadUrl: String,
        val assetName: String,
    )

    /**
     * AG thread'inde (network) calisir - cagiran taraf coroutine icinde
     * Dispatchers.IO ile sarmalamali. Herhangi bir hata/yeni surum yoksa
     * sessizce null doner (kullaniciyi rahatsiz etmez).
     */
    fun checkForUpdate(currentVersionName: String): UpdateInfo? {
        return try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            val version = tag.removePrefix("v")
            if (version.isBlank() || !isNewer(version, currentVersionName)) return null

            val assets = json.optJSONArray("assets") ?: return null
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
            val downloadUrl = url ?: return null
            UpdateInfo(
                version = version,
                notes = json.optString("body", ""),
                downloadUrl = downloadUrl,
                assetName = name ?: "update.apk",
            )
        } catch (e: Exception) {
            Log.w("UpdateChecker", "Guncelleme kontrolu basarisiz: ${e.message}")
            null
        }
    }

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

package com.example.balancewidget.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(val version: String, val pageUrl: String, val notes: String)

/** Reads the latest public GitHub Release without requiring GitHub authentication. */
class UpdateChecker {
    suspend fun latestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(API_URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                if (connection.responseCode !in 200..299) error("GitHub 返回 HTTP ${connection.responseCode}")
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                ReleaseInfo(
                    version = json.optString("tag_name").removePrefix("v"),
                    pageUrl = json.optString("html_url", RELEASES_URL),
                    notes = json.optString("body", "")
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        const val REPOSITORY = "uuuu1415/balancewidget-android-uuuu1415"
        const val RELEASES_URL = "https://github.com/$REPOSITORY/releases"
        private const val API_URL = "https://api.github.com/repos/$REPOSITORY/releases/latest"
    }
}

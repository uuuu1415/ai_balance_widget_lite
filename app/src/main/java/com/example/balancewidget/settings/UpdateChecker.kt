package com.example.balancewidget.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val version: String,
    val pageUrl: String,
    val notes: String,
    val apkUrl: String?
)

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
                    notes = json.optString("body", ""),
                    apkUrl = json.optJSONArray("assets")
                        ?.let { assets ->
                            (0 until assets.length())
                                .map { assets.getJSONObject(it) }
                                .firstOrNull { asset -> asset.optString("name").endsWith(".apk", ignoreCase = true) }
                                ?.optString("browser_download_url")
                                ?.takeIf { it.startsWith("https://github.com/") }
                        }
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

        /** Compares numeric semantic-version segments and ignores a leading v. */
        fun isNewer(candidate: String, current: String): Boolean {
            val candidateParts = candidate.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
            val currentParts = current.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
            val length = maxOf(candidateParts.size, currentParts.size)
            return (0 until length).firstOrNull { index ->
                (candidateParts.getOrElse(index) { 0 }) != (currentParts.getOrElse(index) { 0 })
            }?.let { index -> candidateParts.getOrElse(index) { 0 } > currentParts.getOrElse(index) { 0 } } ?: false
        }
    }
}

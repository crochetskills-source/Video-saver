package com.ytdlp.app.data.preferences

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ytdlp_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val DEFAULT_SERVER_URL = "https://web-production-19d55cca.up.railway.app"
        private const val KEY_DEFAULT_FORMAT = "default_format"
        private const val KEY_EMBED_THUMBNAIL = "embed_thumbnail"
        private const val KEY_EMBED_METADATA = "embed_metadata"
        private const val KEY_SPONSORBLOCK = "sponsorblock"
    }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim()).apply()

    var defaultFormat: String
        get() = prefs.getString(KEY_DEFAULT_FORMAT, "bestvideo*+bestaudio/best") ?: "bestvideo*+bestaudio/best"
        set(value) = prefs.edit().putString(KEY_DEFAULT_FORMAT, value).apply()

    var embedThumbnail: Boolean
        get() = prefs.getBoolean(KEY_EMBED_THUMBNAIL, true)
        set(value) = prefs.edit().putBoolean(KEY_EMBED_THUMBNAIL, value).apply()

    var embedMetadata: Boolean
        get() = prefs.getBoolean(KEY_EMBED_METADATA, true)
        set(value) = prefs.edit().putBoolean(KEY_EMBED_METADATA, value).apply()

    var enableSponsorBlock: Boolean
        get() = prefs.getBoolean(KEY_SPONSORBLOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_SPONSORBLOCK, value).apply()
}

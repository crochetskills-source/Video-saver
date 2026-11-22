package com.ytdlp.app.data.models

import com.google.gson.annotations.SerializedName

data class FormatInfo(
    @SerializedName("format_id") val formatId: String,
    @SerializedName("ext") val ext: String,
    @SerializedName("resolution") val resolution: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("fps") val fps: Double? = null,
    @SerializedName("vcodec") val vcodec: String? = null,
    @SerializedName("acodec") val acodec: String? = null,
    @SerializedName("filesize") val filesize: Long? = null,
    @SerializedName("filesize_approx") val filesizeApprox: Long? = null,
    @SerializedName("tbr") val tbr: Double? = null,
    @SerializedName("format_note") val formatNote: String? = null,
    @SerializedName("has_video") val hasVideo: Boolean = false,
    @SerializedName("has_audio") val hasAudio: Boolean = false
) {
    val displaySize: String
        get() {
            val bytes = filesize ?: filesizeApprox ?: return "Unknown size"
            val mb = bytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024)
            } else {
                String.format("%.1f MB", mb)
            }
        }

    val label: String
        get() {
            val res = resolution ?: "audio"
            val note = formatNote?.let { " ($it)" } ?: ""
            val fpsStr = if (fps != null && fps > 30) " ${fps.toInt()}fps" else ""
            return "$res$fpsStr - $ext$note • $displaySize"
        }
}

data class SubtitleTrack(
    @SerializedName("lang") val lang: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("ext") val ext: String? = null
)

data class VideoMetadata(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("uploader") val uploader: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("duration") val duration: Int? = 0,
    @SerializedName("duration_string") val durationString: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("view_count") val viewCount: Long? = 0,
    @SerializedName("webpage_url") val webpageUrl: String,
    @SerializedName("formats") val formats: List<FormatInfo> = emptyList(),
    @SerializedName("subtitles") val subtitles: List<SubtitleTrack> = emptyList(),
    @SerializedName("automatic_captions") val automaticCaptions: List<SubtitleTrack> = emptyList()
)

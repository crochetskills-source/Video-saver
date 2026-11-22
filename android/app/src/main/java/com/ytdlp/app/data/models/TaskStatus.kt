package com.ytdlp.app.data.models

import com.google.gson.annotations.SerializedName

data class TaskStatus(
    @SerializedName("task_id") val taskId: String,
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("status") val status: String, // "queued", "downloading", "processing", "completed", "error"
    @SerializedName("progress") val progress: Float = 0f,
    @SerializedName("downloaded_bytes") val downloadedBytes: Long? = 0,
    @SerializedName("total_bytes") val totalBytes: Long? = 0,
    @SerializedName("speed") val speed: Double? = 0.0,
    @SerializedName("speed_str") val speedStr: String? = null,
    @SerializedName("eta") val eta: Int? = 0,
    @SerializedName("eta_str") val etaStr: String? = null,
    @SerializedName("filename") val filename: String? = null,
    @SerializedName("error") val error: String? = null
) {
    val isFinished: Boolean
        get() = status == "completed" || status == "error"

    val isSuccess: Boolean
        get() = status == "completed"
}

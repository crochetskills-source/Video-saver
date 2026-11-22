package com.ytdlp.app.data.api

import com.ytdlp.app.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface YtDlpApiService {

    @GET("/health")
    suspend fun healthCheck(): Response<Map<String, String>>

    @POST("/api/extract")
    suspend fun extractMetadata(@Body request: ExtractRequest): Response<VideoMetadata>

    @POST("/api/download")
    suspend fun startDownload(@Body request: DownloadRequest): Response<DownloadResponse>

    @GET("/api/tasks/{task_id}")
    suspend fun getTaskStatus(@Path("task_id") taskId: String): Response<TaskStatus>

    @GET("/api/tasks")
    suspend fun listTasks(): Response<List<TaskStatus>>

    @DELETE("/api/tasks/{task_id}")
    suspend fun deleteTask(@Path("task_id") taskId: String): Response<Map<String, String>>
}

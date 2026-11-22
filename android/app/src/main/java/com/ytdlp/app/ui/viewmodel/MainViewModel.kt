package com.ytdlp.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ytdlp.app.data.api.NetworkClient
import com.ytdlp.app.data.models.*
import com.ytdlp.app.data.preferences.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class MetadataLoaded(val metadata: VideoMetadata) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskStatus>>(emptyList())
    val tasks: StateFlow<List<TaskStatus>> = _tasks.asStateFlow()

    private val _isServerConnected = MutableStateFlow<Boolean?>(null)
    val isServerConnected: StateFlow<Boolean?> = _isServerConnected.asStateFlow()

    private var pollingJob: Job? = null

    init {
        checkServerHealth()
        startTasksPolling()
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            try {
                val service = NetworkClient.getService(prefs.serverUrl)
                val response = service.healthCheck()
                _isServerConnected.value = response.isSuccessful
            } catch (e: Exception) {
                _isServerConnected.value = false
            }
        }
    }

    fun extractUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val service = NetworkClient.getService(prefs.serverUrl)
                val response = service.extractMetadata(ExtractRequest(url.trim()))
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = UiState.MetadataLoaded(response.body()!!)
                } else {
                    _uiState.value = UiState.Error(response.errorBody()?.string() ?: "Failed to extract metadata")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Network connection error")
            }
        }
    }

    fun startDownload(request: DownloadRequest, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val service = NetworkClient.getService(prefs.serverUrl)
                val response = service.startDownload(request)
                if (response.isSuccessful && response.body() != null) {
                    val taskId = response.body()!!.taskId
                    refreshTasks()
                    onSuccess(taskId)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Download request failed: ${e.localizedMessage}")
            }
        }
    }

    fun refreshTasks() {
        viewModelScope.launch {
            try {
                val service = NetworkClient.getService(prefs.serverUrl)
                val response = service.listTasks()
                if (response.isSuccessful) {
                    _tasks.value = response.body()?.reversed() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    private fun startTasksPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val service = NetworkClient.getService(prefs.serverUrl)
                    val response = service.listTasks()
                    if (response.isSuccessful) {
                        _tasks.value = response.body()?.reversed() ?: emptyList()
                    }
                } catch (_: Exception) {}
                delay(1500)
            }
        }
    }

    fun clearState() {
        _uiState.value = UiState.Idle
    }
}

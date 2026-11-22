package com.ytdlp.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ytdlp.app.ui.screens.*
import com.ytdlp.app.ui.theme.YtDlpTheme
import com.ytdlp.app.ui.viewmodel.MainViewModel
import com.ytdlp.app.ui.viewmodel.UiState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capture shared video URL from other apps (e.g. YouTube, Twitter, Instagram, TikTok)
        val sharedUrl = extractSharedUrl(intent)

        setContent {
            YtDlpTheme {
                MainAppNavHost(
                    viewModel = viewModel,
                    initialSharedUrl = sharedUrl
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val sharedUrl = extractSharedUrl(intent)
        if (sharedUrl.isNotBlank()) {
            viewModel.extractUrl(sharedUrl)
        }
    }

    private fun extractSharedUrl(intent: Intent?): String {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            // Extract URL from shared text if surrounded by other characters
            val urlPattern = """https?://[^\s]+""".toRegex()
            return urlPattern.find(text)?.value ?: text
        }
        return ""
    }
}

@Composable
fun MainAppNavHost(
    viewModel: MainViewModel,
    initialSharedUrl: String = ""
) {
    var currentTab by remember { mutableStateOf(0) } // 0: Home, 1: Downloads, 2: Settings
    var showingMediaDetail by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            if (!showingMediaDetail) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.CloudDownload, contentDescription = "Downloads") },
                        label = { Text("Downloads") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showingMediaDetail && uiState is UiState.MetadataLoaded) {
                MediaDetailScreen(
                    metadata = (uiState as UiState.MetadataLoaded).metadata,
                    viewModel = viewModel,
                    onBack = {
                        showingMediaDetail = false
                        viewModel.clearState()
                    },
                    onDownloadStarted = {
                        showingMediaDetail = false
                        currentTab = 1 // Switch to Downloads tab
                    }
                )
            } else {
                when (currentTab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        initialUrl = initialSharedUrl,
                        onNavigateToMediaDetail = { showingMediaDetail = true },
                        onNavigateToSettings = { currentTab = 2 }
                    )
                    1 -> TasksScreen(viewModel = viewModel)
                    2 -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentTab = 0 }
                    )
                }
            }
        }
    }
}

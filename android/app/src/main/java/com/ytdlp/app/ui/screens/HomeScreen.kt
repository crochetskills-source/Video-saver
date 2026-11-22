package com.ytdlp.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ytdlp.app.ui.theme.AccentGreen
import com.ytdlp.app.ui.theme.RedPrimary
import com.ytdlp.app.ui.viewmodel.MainViewModel
import com.ytdlp.app.ui.viewmodel.UiState

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    initialUrl: String = "",
    onNavigateToMediaDetail: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf(initialUrl) }
    val uiState by viewModel.uiState.collectAsState()
    val isServerConnected by viewModel.isServerConnected.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.MetadataLoaded) {
            onNavigateToMediaDetail()
        }
    }

    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) {
            urlText = initialUrl
            viewModel.extractUrl(initialUrl)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Server status banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when (isServerConnected) {
                                    true -> AccentGreen
                                    false -> RedPrimary
                                    else -> Color.Gray
                                },
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (isServerConnected) {
                            true -> "Railway Server Online"
                            false -> "Server Offline / Unreachable"
                            else -> "Connecting to Server..."
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "yt-dlp Downloader",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Download any video or audio at maximum quality with ffmpeg merging & post-processing",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // URL Input box
        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Paste video, playlist, or media URL") },
            placeholder = { Text("https://www.youtube.com/watch?v=...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip?.let { clip ->
                        if (clip.itemCount > 0) {
                            val pasted = clip.getItemAt(0).text?.toString() ?: ""
                            urlText = pasted
                        }
                    }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste from Clipboard")
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Analyze button
        Button(
            onClick = {
                if (urlText.isNotBlank()) {
                    viewModel.extractUrl(urlText)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = urlText.isNotBlank() && uiState !is UiState.Loading
        ) {
            if (uiState is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing formats & streams...")
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetch Media Info", fontSize = 16.sp)
            }
        }

        // Error message if any
        if (uiState is UiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = (uiState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp
                )
            }
        }
    }
}

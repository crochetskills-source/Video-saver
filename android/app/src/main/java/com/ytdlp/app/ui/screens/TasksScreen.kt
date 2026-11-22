package com.ytdlp.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ytdlp.app.data.api.NetworkClient
import com.ytdlp.app.data.download.DownloadHelper
import com.ytdlp.app.data.models.TaskStatus
import com.ytdlp.app.ui.theme.AccentGreen
import com.ytdlp.app.ui.theme.RedPrimary
import com.ytdlp.app.ui.viewmodel.MainViewModel

@Composable
fun TasksScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Downloads & Tasks",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.refreshTasks() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active downloads yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.taskId }) { task ->
                    TaskCard(
                        task = task,
                        onSaveToDevice = {
                            val downloadUrl = NetworkClient.getDownloadFileUrl(
                                viewModel.prefs.serverUrl,
                                task.taskId
                            )
                            val name = task.filename ?: "${task.taskId}_media.mp4"
                            DownloadHelper.downloadFileToDevice(context, downloadUrl, name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskStatus,
    onSaveToDevice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = task.title ?: task.url,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status chip and info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (task.status) {
                        "downloading" -> "Downloading: ${task.progress.toInt()}%"
                        "processing" -> "Merging & Post-Processing..."
                        "completed" -> "Ready to Save"
                        "error" -> "Failed"
                        else -> "Queued"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (task.status) {
                        "completed" -> AccentGreen
                        "error" -> RedPrimary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )

                if (task.status == "downloading") {
                    Text(
                        text = "${task.speedStr ?: ""} • ETA: ${task.etaStr ?: ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            when (task.status) {
                "downloading" -> {
                    LinearProgressIndicator(
                        progress = { task.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                    )
                }
                "processing" -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                }
                "completed" -> {
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = AccentGreen
                    )
                }
                else -> {}
            }

            if (task.status == "completed") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onSaveToDevice,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DownloadForOffline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Phone Downloads")
                }
            }

            if (task.status == "error" && !task.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.error,
                    fontSize = 12.sp,
                    color = RedPrimary
                )
            }
        }
    }
}

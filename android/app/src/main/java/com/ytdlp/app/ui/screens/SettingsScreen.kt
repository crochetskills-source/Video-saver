package com.ytdlp.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ytdlp.app.ui.theme.AccentGreen
import com.ytdlp.app.ui.theme.RedPrimary
import com.ytdlp.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var serverUrlText by remember { mutableStateOf(viewModel.prefs.serverUrl) }
    val isConnected by viewModel.isServerConnected.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Railway Backend Server",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enter the public domain URL of your Railway container (e.g., https://your-app.up.railway.app)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = serverUrlText,
                onValueChange = { serverUrlText = it },
                label = { Text("Railway Server Base URL") },
                placeholder = { Text("https://xxx.up.railway.app") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.prefs.serverUrl = serverUrlText
                        viewModel.checkServerHealth()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Connect")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection status indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (isConnected) {
                            true -> Icons.Default.CheckCircle
                            false -> Icons.Default.Error
                            else -> Icons.Default.Sync
                        },
                        contentDescription = null,
                        tint = when (isConnected) {
                            true -> AccentGreen
                            false -> RedPrimary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (isConnected) {
                            true -> "Connected successfully to Railway Server"
                            false -> "Connection failed. Please check URL & deployment."
                            else -> "Checking status..."
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "How to deploy server on Railway:",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "1. Push the 'server' folder to your GitHub.\n2. In railway.com, click 'New Project' -> 'Deploy from GitHub'.\n3. Railway automatically detects the Dockerfile and builds FFmpeg + yt-dlp.\n4. Go to Settings -> Networking -> Generate Domain and paste that URL above.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

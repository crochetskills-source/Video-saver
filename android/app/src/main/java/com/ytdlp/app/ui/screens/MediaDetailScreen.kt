package com.ytdlp.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ytdlp.app.data.models.DownloadRequest
import com.ytdlp.app.data.models.VideoMetadata
import com.ytdlp.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    metadata: VideoMetadata,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onDownloadStarted: () -> Unit
) {
    var downloadMode by remember { mutableStateOf(0) } // 0: Video Presets, 1: Audio Only, 2: Advanced
    
    // Video quality preset selector
    var selectedVideoPreset by remember { mutableStateOf("bestvideo*+bestaudio/best") }
    
    // Audio options
    var audioFormat by remember { mutableStateOf("mp3") }
    var audioQuality by remember { mutableStateOf("320") }
    
    // Advanced custom format
    var customFormatString by remember { mutableStateOf("bestvideo*+bestaudio/best") }
    
    // Post-processing options
    var embedThumbnail by remember { mutableStateOf(true) }
    var embedSubtitles by remember { mutableStateOf(false) }
    var embedMetadata by remember { mutableStateOf(true) }
    var enableSponsorBlock by remember { mutableStateOf(false) }
    var mergeFormat by remember { mutableStateOf("mp4") }
    
    // Clip trimming
    var clipStart by remember { mutableStateOf("") }
    var clipEnd by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Options", maxLines = 1) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Media Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    if (!metadata.thumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = metadata.thumbnail,
                            contentDescription = metadata.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = metadata.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = metadata.channel ?: metadata.uploader ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = metadata.durationString ?: "",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download Mode Tabs
            PrimaryTabRow(selectedTabIndex = downloadMode) {
                Tab(
                    selected = downloadMode == 0,
                    onClick = { downloadMode = 0 },
                    text = { Text("Video") }
                )
                Tab(
                    selected = downloadMode == 1,
                    onClick = { downloadMode = 1 },
                    text = { Text("Audio Only") }
                )
                Tab(
                    selected = downloadMode == 2,
                    onClick = { downloadMode = 2 },
                    text = { Text("Advanced") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (downloadMode) {
                0 -> {
                    // Video Presets
                    Text("Select Resolution (Merged with Best Audio):", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val presets = listOf(
                        "Best Available (Max 4K/8K)" to "bestvideo*+bestaudio/best",
                        "1080p Full HD" to "bestvideo[height<=1080]+bestaudio/best[height<=1080]",
                        "720p HD" to "bestvideo[height<=720]+bestaudio/best[height<=720]",
                        "480p SD" to "bestvideo[height<=480]+bestaudio/best[height<=480]"
                    )

                    presets.forEach { (label, selector) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedVideoPreset == selector,
                                onClick = { selectedVideoPreset = selector }
                            )
                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                1 -> {
                    // Audio Only
                    Text("Audio Format & Quality:", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("mp3", "m4a", "flac", "opus").forEach { fmt ->
                            FilterChip(
                                selected = audioFormat == fmt,
                                onClick = { audioFormat = fmt },
                                label = { Text(fmt.uppercase()) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bitrate Quality:", fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("320", "256", "192", "128").forEach { kbps ->
                            FilterChip(
                                selected = audioQuality == kbps,
                                onClick = { audioQuality = kbps },
                                label = { Text("${kbps} kbps") }
                            )
                        }
                    }
                }
                2 -> {
                    // Advanced Format Selector
                    Text("Custom yt-dlp Format Selector:", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customFormatString,
                        onValueChange = { customFormatString = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Format String (-f)") },
                        placeholder = { Text("e.g. bv*[ext=mp4]+ba[ext=m4a]/b") }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Post-Processing Options
            Text("Post-Processing & Enhancement:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = embedThumbnail, onCheckedChange = { embedThumbnail = it })
                Text("Embed Thumbnail Art into File")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = embedMetadata, onCheckedChange = { embedMetadata = it })
                Text("Embed Metadata & Chapter Markers")
            }

            if (downloadMode != 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = embedSubtitles, onCheckedChange = { embedSubtitles = it })
                    Text("Embed Subtitles (Closed Captions)")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = enableSponsorBlock, onCheckedChange = { enableSponsorBlock = it })
                Text("SponsorBlock (Auto-skip sponsors & intros)")
            }

            // Trimming (Optional)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Trim Clip (Optional):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = clipStart,
                    onValueChange = { clipStart = it },
                    label = { Text("Start (00:01:30)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = clipEnd,
                    onValueChange = { clipEnd = it },
                    label = { Text("End (00:03:00)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start Download Button
            Button(
                onClick = {
                    val finalFormat = when (downloadMode) {
                        0 -> selectedVideoPreset
                        1 -> "bestaudio/best"
                        else -> customFormatString
                    }

                    val req = DownloadRequest(
                        url = metadata.webpageUrl,
                        formatSelector = finalFormat,
                        audioOnly = downloadMode == 1,
                        audioFormat = if (downloadMode == 1) audioFormat else null,
                        audioQuality = if (downloadMode == 1) audioQuality else null,
                        mergeOutputFormat = mergeFormat,
                        embedThumbnail = embedThumbnail,
                        embedSubtitles = embedSubtitles,
                        embedMetadata = embedMetadata,
                        sponsorblockRemove = if (enableSponsorBlock) listOf("sponsor", "intro", "outro") else null,
                        clipStart = clipStart.ifBlank { null },
                        clipEnd = clipEnd.ifBlank { null }
                    )

                    viewModel.startDownload(req) {
                        onDownloadStarted()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Server Download", fontSize = 16.sp)
            }
        }
    }
}

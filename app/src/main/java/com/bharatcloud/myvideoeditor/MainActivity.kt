package com.bharatcloud.myvideoeditor

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

private val Canvas = Color(0xFF0B1118)
private val SurfaceDark = Color(0xFF17232E)
private val Accent = Color(0xFF22D3C5)
private val Muted = Color(0xFFA9BAC7)
private val Ink = Color(0xFF081116)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 17, 24)
        window.navigationBarColor = android.graphics.Color.rgb(11, 17, 24)
        setContent { TypographyMakerScreen() }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun TypographyMakerScreen() {
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var overlayText by remember { mutableStateOf("YOUR STORY\nSTARTS HERE") }
    var selectedStyle by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var showExportInfo by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableIntStateOf(0) }
    var clipStart by remember { mutableFloatStateOf(0f) }
    var clipEnd by remember { mutableFloatStateOf(15f) }
    var clips by remember { mutableStateOf(listOf(TextClip(1, "YOUR STORY\nSTARTS HERE", 0f, 15f, 0))) }
    var selectedClip by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val styles = listOf("BOLD", "CLEAN", "OUTLINE", "EDITORIAL")
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> videoUri = uri }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Canvas, surface = SurfaceDark, onPrimary = Ink, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Canvas) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TYPOGRAPHY", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("Video Maker", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(color = SurfaceDark, shape = RoundedCornerShape(14.dp)) {
                        Text("BETA", color = Accent, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(22.dp)).border(1.dp, Color(0xFF263744), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    if (videoUri != null) VideoPreview(videoUri!!, onPlayingChanged = { isPlaying = it })
                    else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.VideoLibrary, null, tint = Accent, modifier = Modifier.size(48.dp))
                        Text("Your video preview", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Import a clip to start designing", color = Muted, fontSize = 12.sp)
                    }
                    if (overlayText.isNotBlank()) Text(overlayText, modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 20.dp), color = if (selectedStyle == 2) Accent else Color.White, fontSize = if (selectedStyle == 3) 25.sp else 30.sp, fontWeight = if (selectedStyle == 1 || selectedStyle == 3) FontWeight.Medium else FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 34.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { picker.launch(arrayOf("video/*")) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Default.Add, null, tint = Ink)
                        Spacer(Modifier.width(7.dp))
                        Text(if (videoUri == null) "Import video" else "Change video", color = Ink, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { isPlaying = !isPlaying }, enabled = videoUri != null, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YOUR TEXT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Surface(color = SurfaceDark, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(value = overlayText, onValueChange = { value ->
                            overlayText = value
                            clips = clips.toMutableList().also { list -> if (list.isNotEmpty() && selectedClip in list.indices) list[selectedClip] = list[selectedClip].copy(text = value) }
                        }, modifier = Modifier.fillMaxWidth().padding(14.dp), textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold), decorationBox = { inner ->
                            Box { if (overlayText.isEmpty()) Text("Type your video title...", color = Muted); inner() }
                        })
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TYPOGRAPHY STYLES", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        styles.forEachIndexed { index, label ->
                            FilterChip(selected = selectedStyle == index, onClick = {
                                selectedStyle = index
                                clips = clips.toMutableList().also { list -> if (list.isNotEmpty() && selectedClip in list.indices) list[selectedClip] = list[selectedClip].copy(style = index) }
                            }, label = { Text(label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = Ink, labelColor = Color.White))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TIMELINE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            val nextId = (clips.maxOfOrNull { it.id } ?: 0) + 1
                            clips = clips + TextClip(nextId, "NEW TEXT", 0f, 15f, 0)
                            selectedClip = clips.lastIndex
                            overlayText = "NEW TEXT"; clipStart = 0f; clipEnd = 15f; selectedStyle = 0
                        }) { Text("+ Add layer", color = Accent) }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        clips.forEachIndexed { index, clip ->
                            FilterChip(selected = selectedClip == index, onClick = {
                                selectedClip = index; overlayText = clip.text; clipStart = clip.startSeconds; clipEnd = clip.endSeconds; selectedStyle = clip.style
                            }, label = { Text("Text ${index + 1}") })
                        }
                    }
                    Text("Start: ${clipStart.toInt()}s", color = Muted, fontSize = 11.sp)
                    Slider(value = clipStart, onValueChange = { value ->
                        clipStart = value.coerceAtMost(clipEnd - 0.5f)
                        clips = clips.toMutableList().also { it[selectedClip] = it[selectedClip].copy(startSeconds = clipStart) }
                    }, valueRange = 0f..60f)
                    Text("End: ${clipEnd.toInt()}s", color = Muted, fontSize = 11.sp)
                    Slider(value = clipEnd, onValueChange = { value ->
                        clipEnd = value.coerceAtLeast(clipStart + 0.5f)
                        clips = clips.toMutableList().also { it[selectedClip] = it[selectedClip].copy(endSeconds = clipEnd) }
                    }, valueRange = 0.5f..60f)
                    Text("Each text layer appears only during its selected time range. Current timeline limit: 60 seconds.", color = Muted, fontSize = 10.sp)
                }
                Button(onClick = {
                    val source = videoUri
                    if (source == null) Toast.makeText(context, "Import a video first", Toast.LENGTH_SHORT).show()
                    else {
                        exporting = true; exportProgress = 1
                        VideoExporter.export(context, source, clips, { progress -> exportProgress = progress }, { file ->
                            exporting = false; Toast.makeText(context, "Export saved: ${file.name}", Toast.LENGTH_LONG).show()
                        }, { error ->
                            exporting = false; Toast.makeText(context, "Export failed: $error", Toast.LENGTH_LONG).show()
                        })
                    }
                }, enabled = videoUri != null && !exporting, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263744)), shape = RoundedCornerShape(15.dp)) {
                    Text(if (exporting) "Exporting..." else "Export MP4", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text("Bharat Cloud Technology", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 5.dp))
            }
        }
        if (showExportInfo) AlertDialog(onDismissRequest = { showExportInfo = false }, title = { Text("MP4 Export") }, text = { Text("Select a video, add text layers, set each layer's start and end time, then tap Export MP4. Exported files are saved in this app's Movies folder.") }, confirmButton = { TextButton(onClick = { showExportInfo = false }) { Text("Got it") } }, containerColor = SurfaceDark, titleContentColor = Color.White, textContentColor = Muted)
    }
}

@Composable
private fun VideoPreview(uri: Uri, onPlayingChanged: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri)); prepare(); playWhenReady = false
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = onPlayingChanged(isPlaying)
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    AndroidView(factory = { ctx ->
        PlayerView(ctx).apply {
            this.player = player; useController = true
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
}

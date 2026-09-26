package com.example.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.AiStudioViewModel
import com.example.ui.theme.*

@Composable
fun VideoScreen(
    viewModel: AiStudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoPrompt by viewModel.videoPrompt.collectAsState()
    val videoDuration by viewModel.videoDurationSec.collectAsState()
    val videoQuality by viewModel.videoQuality.collectAsState()
    val videoStyle by viewModel.videoStyle.collectAsState()
    val isGenerating by viewModel.isVideoGenerating.collectAsState()
    val videoStage by viewModel.videoStage.collectAsState()
    val videoProgressPercentage by viewModel.videoProgressPercentage.collectAsState()
    val currentVideoUrl by viewModel.currentVideoUrl.collectAsState()
    val currentVideoPosterUrl by viewModel.currentVideoPosterUrl.collectAsState()
    val savedVideos by viewModel.savedVideos.collectAsState()

    val styles = listOf("Cinematic", "Sci-Fi Cyber", "Hyper-Realistic", "Anime Motion")
    val qualities = listOf("720p HD", "1080p Full HD")
    val durations = listOf(5, 10, 15, 30)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "استوديو توليد الفيديو بالذكاء الاصطناعي",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "حول المشاهد والقصص إلى مقاطع فيديو متحركة بدقة سينمائية",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Surface(
                color = SurfaceVariantDark,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Veo 3.1 & Luma",
                        tint = SecondaryCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Veo 3.1",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCyan
                    )
                }
            }
        }

        // Prompt Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وصف المشهد المرئي (Scene Description)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    TextButton(
                        onClick = { viewModel.setRandomVideoPrompt() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("random_video_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "فكرة عشوائية",
                            modifier = Modifier.size(16.dp),
                            tint = SecondaryCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مشهد مقترح 🎬",
                            fontSize = 12.sp,
                            color = SecondaryCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = videoPrompt,
                    onValueChange = { viewModel.updateVideoPrompt(it) },
                    placeholder = {
                        Text(
                            text = "مثال: طيران بكاميرا سينمائية درون فوق مدينة سايبر بانك ممطرة ومضيئة بالنيون...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp)
                        .testTag("video_prompt_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedBorderColor = SecondaryCyan,
                        unfocusedBorderColor = CardBorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        // Settings (Duration, Quality, Style)
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Duration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "مدة الفيديو (Duration)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        durations.forEach { d ->
                            val isSelected = videoDuration == d
                            OutlinedButton(
                                onClick = { viewModel.selectVideoDuration(d) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) SecondaryCyan.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    contentColor = if (isSelected) SecondaryCyan else TextSecondary
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) SecondaryCyan else CardBorderDark
                                )
                            ) {
                                Text(
                                    text = "$d ث",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Quality
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "جودة الريندر (Resolution)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        qualities.forEach { q ->
                            val isSelected = videoQuality.contains(q.take(4))
                            OutlinedButton(
                                onClick = { viewModel.selectVideoQuality(q) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) PrimaryNeon.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    contentColor = if (isSelected) PrimaryNeon else TextSecondary
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryNeon else CardBorderDark
                                )
                            ) {
                                Text(q, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // Style
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "النمط السينمائي (Cinematic Style)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(styles) { s ->
                            val isSelected = videoStyle == s
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectVideoStyle(s) },
                                label = { Text(s, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentEmerald,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SurfaceVariantDark,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) AccentEmerald else CardBorderDark
                                )
                            )
                        }
                    }
                }
            }
        }

        // Generate Button
        val isReady = videoPrompt.isNotBlank() && !isGenerating
        Button(
            onClick = { viewModel.generateVideo() },
            enabled = isReady,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = CardBorderDark
            ),
            contentPadding = PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    brush = if (isReady) Brush.horizontalGradient(
                        listOf(SecondaryCyan, PrimaryNeon, AccentPink)
                    ) else Brush.horizontalGradient(
                        listOf(CardBorderDark, CardBorderDark)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .minimumInteractiveComponentSize()
                .testTag("generate_video_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "جارٍ إنتاج الفيديو بالذكاء الاصطناعي...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "بدء توليد الفيديو",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Generating Progress Stages
        if (isGenerating) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "جارٍ إنتاج الفيديو العالي الدقة... 🎬",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Surface(
                            color = SecondaryCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$videoProgressPercentage%",
                                color = SecondaryCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { videoProgressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = SecondaryCyan,
                        trackColor = SurfaceVariantDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = videoStage,
                        color = SecondaryCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "محاكاة الإضاءة والحركة الفيزيائية لمدة $videoDuration ثوانٍ بدقة $videoQuality",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Generated Video Player Card
        if (currentVideoUrl != null && !isGenerating) {
            var isVideoLoaded by remember(currentVideoUrl) { mutableStateOf(false) }
            var isBuffering by remember(currentVideoUrl) { mutableStateOf(true) }
            var playbackError by remember(currentVideoUrl) { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مشغل الفيديو المولد 🎬",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Surface(
                            color = SecondaryCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$videoQuality • $videoDuration ثوانٍ",
                                color = SecondaryCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Embedded Video Player (Safe from black screen)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Poster image underneath prevents black screen before/during buffer
                        if (!currentVideoPosterUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = currentVideoPosterUrl,
                                contentDescription = "معاينة الفيديو",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isVideoLoaded = true
                                            isBuffering = false
                                        }
                                    }
                                }
                            },
                            update = { webView ->
                                if (webView.tag != currentVideoUrl && currentVideoUrl != null) {
                                    webView.tag = currentVideoUrl
                                    isBuffering = true
                                    val safePoster = currentVideoPosterUrl ?: ""
                                    val htmlData = """
                                        <!DOCTYPE html>
                                        <html lang="ar">
                                        <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                        <style>
                                          * { box-sizing: border-box; margin: 0; padding: 0; }
                                          body, html { width: 100%; height: 100%; background: #000000; overflow: hidden; display: flex; justify-content: center; align-items: center; }
                                          video { width: 100%; height: 100%; object-fit: cover; border-radius: 12px; }
                                        </style>
                                        </head>
                                        <body>
                                          <video id="player" src="$currentVideoUrl" poster="$safePoster" controls autoplay loop playsinline preload="auto"></video>
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    webView.loadDataWithBaseURL("https://nova.ai", htmlData, "text/html", "UTF-8", null)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Buffering indicator
                        if (isBuffering && !playbackError) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = SecondaryCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "جارٍ بدء العرض السينمائي...",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                currentVideoUrl?.let { url ->
                                    viewModel.shareVideo(context, url, videoPrompt)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("share_video_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة الفيديو",
                                modifier = Modifier.size(18.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة المقطع", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.analyticsManager.trackVideoDownloaded()
                                android.widget.Toast.makeText(
                                    context,
                                    "جارٍ تنزيل ملف الفيديو بجودة عالية...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("download_video_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceVariantDark,
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "تنزيل الفيديو",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تنزيل الفيديو", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Previous Videos History
        if (savedVideos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "سجل الفيديوهات السابقة (${savedVideos.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(savedVideos, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable {
                                    viewModel.loadSavedVideo(
                                        url = item.videoUrl,
                                        prompt = item.prompt,
                                        style = item.style,
                                        duration = item.durationSec
                                    )
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(75.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceVariantDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل",
                                        tint = SecondaryCyan,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.prompt,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    maxLines = 2
                                )
                                Text(
                                    text = "${item.style} • ${item.durationSec}s",
                                    fontSize = 10.sp,
                                    color = AccentEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

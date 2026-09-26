package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.TextToSpeechHelper
import com.example.util.VoiceInputManager
import com.example.util.VoiceState

@Composable
fun VoiceAssistantModeDialog(
    voiceState: VoiceState,
    ttsHelper: TextToSpeechHelper,
    isAiResponding: Boolean,
    lastAiResponse: String?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSubmitVoiceQuery: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTtsSpeaking by ttsHelper.isSpeaking.collectAsState()
    var continuousMode by remember { mutableStateOf(true) }
    var soundMuted by remember { mutableStateOf(false) }
    var initialGreetingPlayed by remember { mutableStateOf(false) }

    // Initial warm human welcome when voice conversation starts
    LaunchedEffect(Unit) {
        if (!initialGreetingPlayed) {
            initialGreetingPlayed = true
            ttsHelper.speak(
                rawText = "أهلاً بيك! أنا سامعك بكل وضوح، احكيلي عاوز إيه وهرد عليك فوراً.",
                onDone = {
                    if (continuousMode) {
                        onStartListening()
                    }
                }
            )
        }
    }

    // Auto-send recognized voice query for natural hands-free back-and-forth
    LaunchedEffect(voiceState.text) {
        if (continuousMode && voiceState.text.isNotBlank() && !isAiResponding && !isTtsSpeaking) {
            val spoken = voiceState.text
            onStopListening()
            onSubmitVoiceQuery(spoken)
        }
    }

    // Pulsing halo animation
    val infiniteTransition = rememberInfiniteTransition(label = "voice_mode_halo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    // When AI response completes, speak it if not muted
    LaunchedEffect(lastAiResponse, isAiResponding) {
        if (!isAiResponding && !lastAiResponse.isNullOrBlank() && !soundMuted) {
            ttsHelper.speak(
                rawText = lastAiResponse,
                onDone = {
                    if (continuousMode) {
                        onStartListening()
                    }
                }
            )
        }
    }

    Dialog(
        onDismissRequest = {
            ttsHelper.stop()
            onStopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .testTag("voice_assistant_mode_dialog"),
            color = Color(0xFF070B14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            ttsHelper.stop()
                            onStopListening()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .minimumInteractiveComponentSize()
                            .testTag("exit_voice_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إنهاء الوضع الصوتي",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isAiResponding -> PrimaryNeon
                                            isTtsSpeaking -> AccentEmerald
                                            voiceState.isListening -> SecondaryCyan
                                            else -> Color.Gray
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المحادثة الصوتية المباشرة",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = when {
                                isAiResponding -> "نوفا AI يفكر ويجهز الرد... ⚡"
                                isTtsSpeaking -> "نوفا AI يتحدث الآن... 🔊"
                                voiceState.isListening -> "نوفا يستمع لصوتك الآن... 🎙️"
                                else -> "جاهز للتحدث معك"
                            },
                            color = SecondaryCyan,
                            fontSize = 11.sp
                        )
                    }

                    // Mute / Unmute Button
                    IconButton(
                        onClick = {
                            soundMuted = !soundMuted
                            if (soundMuted) ttsHelper.stop()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_voice_mute_button")
                    ) {
                        Icon(
                            imageVector = if (soundMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (soundMuted) "تفعيل صوت المساعد" else "كتم صوت المساعد",
                            tint = if (soundMuted) ErrorRed else AccentEmerald
                        )
                    }
                }

                // Central Visualizer Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient Breathing Rings
                        val isActive = voiceState.isListening || isTtsSpeaking || isAiResponding

                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .scale(haloPulse)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                (if (isTtsSpeaking) AccentEmerald else SecondaryCyan).copy(alpha = 0.25f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .scale(1f + (voiceState.soundLevel * 0.45f))
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.5.dp,
                                        brush = Brush.linearGradient(
                                            listOf(SecondaryCyan, PrimaryNeon, AccentPink)
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }

                        // Central Glowing Core
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            if (isTtsSpeaking) AccentEmerald else PrimaryNeon,
                                            if (isTtsSpeaking) SecondaryCyan else Color(0xFF0F172A)
                                        )
                                    )
                                )
                                .clickable {
                                    if (voiceState.isListening) {
                                        onStopListening()
                                    } else {
                                        ttsHelper.stop()
                                        onStartListening()
                                    }
                                }
                                .testTag("voice_central_orb"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isTtsSpeaking -> Icons.Default.GraphicEq
                                    isAiResponding -> Icons.Default.AutoAwesome
                                    voiceState.isListening -> Icons.Default.Mic
                                    else -> Icons.Default.MicNone
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Sound Wave Bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(36.dp)
                    ) {
                        val barCount = 11
                        val activeLevel = if (voiceState.isListening) voiceState.soundLevel else if (isTtsSpeaking) 0.6f else 0.1f

                        for (i in 0 until barCount) {
                            val waveMultiplier = (1 + (i % 4)) * 7
                            val barH = (8 + (activeLevel * waveMultiplier)).coerceIn(6f, 32f).dp
                            Box(
                                modifier = Modifier
                                    .width(4.5.dp)
                                    .height(barH)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(SecondaryCyan, PrimaryNeon, AccentPink)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Real-time Captions Preview Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 160.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
                        border = BorderStroke(1.dp, CardBorderDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val activeText = voiceState.partialText.ifBlank { voiceState.text }

                            if (activeText.isNotBlank()) {
                                Text(
                                    text = "🗣️ أنت: \"$activeText\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            } else if (!lastAiResponse.isNullOrBlank()) {
                                Text(
                                    text = "🤖 نوفا AI: ${lastAiResponse.take(180)}${if (lastAiResponse.length > 180) "..." else ""}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryCyan,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp
                                )
                            } else {
                                Text(
                                    text = "تحدث الآن بحرية، وسيقوم نوفا AI بالاستماع والرد عليك صوتياً وفورياً.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Bottom Controls Toolbar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Continuous Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { continuousMode = !continuousMode }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (continuousMode) AccentEmerald else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "المحادثة الصوتية المستمرة (تحدث ورد تلقائي)",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                        Switch(
                            checked = continuousMode,
                            onCheckedChange = { continuousMode = it },
                            modifier = Modifier.testTag("continuous_voice_mode_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AccentEmerald
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mic Button
                        Button(
                            onClick = {
                                if (voiceState.isListening) {
                                    onStopListening()
                                } else {
                                    ttsHelper.stop()
                                    onStartListening()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("voice_dialog_mic_action"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (voiceState.isListening) ErrorRed else PrimaryNeon
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (voiceState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (voiceState.isListening) "إيقاف الاستماع" else "تحدث الآن",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Send query button if text was recognized
                        val hasRecognized = (voiceState.text.isNotBlank() || voiceState.partialText.isNotBlank())
                        if (hasRecognized) {
                            Button(
                                onClick = {
                                    val textToSend = voiceState.text.ifBlank { voiceState.partialText }
                                    onStopListening()
                                    onSubmitVoiceQuery(textToSend)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("voice_dialog_submit_action"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentEmerald
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إرسال لنوفا",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

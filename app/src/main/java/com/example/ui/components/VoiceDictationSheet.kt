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
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.ui.theme.*
import com.example.util.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceDictationSheet(
    voiceState: VoiceState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onDismiss: () -> Unit,
    onInsertText: (String) -> Unit,
    onSendDirectly: (String) -> Unit,
    onLaunchSystemRecognizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val currentText = if (voiceState.partialText.isNotBlank()) {
        voiceState.partialText
    } else if (voiceState.text.isNotBlank()) {
        voiceState.text
    } else {
        ""
    }

    ModalBottomSheet(
        onDismissRequest = {
            onStopListening()
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("voice_dictation_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (voiceState.isListening) ErrorRed else AccentEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (voiceState.isListening) "جاري الاستماع لصوتك..." else "الإملاء الصوتي الذكي",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "عربي / English",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Central Animated Microphone & Wave Visualizer
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (voiceState.isListening) {
                    // Pulsing Outer Rings
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .scale(1f + (voiceState.soundLevel * 0.35f))
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(SecondaryCyan, PrimaryNeon)),
                                shape = CircleShape
                            )
                    )
                }

                // Main Mic Button
                FilledIconButton(
                    onClick = {
                        if (voiceState.isListening) {
                            onStopListening()
                        } else {
                            onStartListening()
                        }
                    },
                    modifier = Modifier
                        .size(76.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("dictation_mic_toggle_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (voiceState.isListening) ErrorRed else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (voiceState.isListening) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = if (voiceState.isListening) "إيقاف التسجيل الصوتي" else "بدء التحدث بالصوت",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Audio Level Waveform Bars
            if (voiceState.isListening) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(28.dp)
                ) {
                    val baseHeights = listOf(10, 16, 24, 18, 12, 22, 14, 20, 10)
                    baseHeights.forEachIndexed { index, baseH ->
                        val dynamicFactor = (voiceState.soundLevel * (index % 3 + 1) * 6).coerceAtMost(16f)
                        val barHeight = (baseH + dynamicFactor).coerceIn(6f, 26f).dp
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(SecondaryCyan, PrimaryNeon)
                                    )
                                )
                        )
                    }
                }
            }

            // Real-time Recognized Speech Display Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 150.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (voiceState.isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentText.isNotBlank()) {
                        Text(
                            text = currentText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.testTag("recognized_speech_text")
                        )
                    } else if (voiceState.errorMessage != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = voiceState.errorMessage,
                                fontSize = 13.sp,
                                color = ErrorRed,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = onLaunchSystemRecognizer,
                                modifier = Modifier.testTag("system_recognizer_fallback_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsVoice,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "استخدام لاقط النظام الصوتي",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (voiceState.isListening)
                                "تحدث الآن بوضوح، وسيقوم نوفا بتحويل صوتك إلى نص فورياً..."
                            else
                                "اضغط على أيقونة الميكروفون بالأعلى وتحدث لإملاء استفسارك",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Action Buttons
            AnimatedVisibility(
                visible = currentText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Send directly to AI
                    Button(
                        onClick = {
                            onStopListening()
                            onSendDirectly(currentText)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("voice_send_directly_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إرسال فوراً",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Insert to text input to edit
                    OutlinedButton(
                        onClick = {
                            onStopListening()
                            onInsertText(currentText)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("voice_insert_to_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إدراج وتعديل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Secondary controls: Cancel / Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onStopListening()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("close_dictation_sheet_button")
                ) {
                    Text(
                        text = "إلغاء",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                if (!voiceState.isListening && currentText.isBlank()) {
                    TextButton(
                        onClick = onLaunchSystemRecognizer,
                        modifier = Modifier.testTag("alt_voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsVoice,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "اللاقط الصوتي المساعد",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

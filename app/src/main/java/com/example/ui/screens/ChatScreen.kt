package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.ChatMessageEntity
import com.example.ui.AiStudioViewModel
import com.example.ui.components.VoiceAssistantModeDialog
import com.example.ui.components.VoiceDictationSheet
import com.example.ui.theme.*
import com.example.util.TextToSpeechHelper
import com.example.util.VoiceInputManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    viewModel: AiStudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val isGenerating by viewModel.isChatGenerating.collectAsState()
    val selectedBitmap by viewModel.selectedImageBitmap.collectAsState()
    val selectedUri by viewModel.selectedImageUri.collectAsState()
    val lastAiResponse by viewModel.lastAiResponse.collectAsState()
    val listState = rememberLazyListState()

    var showClearDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showAudioPermissionDialog by remember { mutableStateOf(false) }
    var showVoiceAssistantMode by remember { mutableStateOf(false) }

    val voiceInputManager = remember { VoiceInputManager(context) }
    val voiceState by voiceInputManager.voiceState.collectAsState()
    val ttsHelper = remember { TextToSpeechHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            voiceInputManager.stopListening()
            ttsHelper.shutdown()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.setSelectedImage(bitmap, null)
            Toast.makeText(context, "تم التقاط الصورة بنجاح 📸", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                viewModel.setSelectedImage(bitmap, uri)
            } catch (_: Exception) {
                viewModel.setSelectedImage(null, uri)
            }
            Toast.makeText(context, "تم إرفاق الصورة للتحليل 🖼️", Toast.LENGTH_SHORT).show()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceSheet = true
            voiceInputManager.startListening(
                onFinalResult = { recognized ->
                    viewModel.appendVoiceInput(recognized)
                }
            )
        } else {
            showAudioPermissionDialog = true
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                viewModel.appendVoiceInput(text)
                Toast.makeText(context, "تم تحويل الصوت بنجاح 🎙️", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { viewModel.saveConversationToUri(context, it) }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size, isGenerating) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "مسح المحادثة",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في مسح جميع رسائل المحادثة الحالية؟",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChat()
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_chat_button")
                ) {
                    Text("مسح الآن", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    modifier = Modifier.testTag("cancel_clear_chat_button")
                ) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "تصدير ومشاركة المحادثة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "يمكنك تصدير كافة رسائل هذه المحادثة (${chatMessages.size} رسالة) كملف نصي (.txt) منسق ومؤرخ.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: Share as file (.txt)
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                viewModel.shareConversationFile(context)
                            }
                            .testTag("export_share_file_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "مشاركة كملف نصي (.txt)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "واتساب، تيليجرام، إيميل، أو جوجل درايف",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: Save directly to device storage (.txt)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                saveFileLauncher.launch("nova_chat_$timeStamp.txt")
                            }
                            .testTag("export_save_device_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "حفظ الملف على الهاتف",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "حفظ مباشرة في مجلد التنزيلات أو المستندات",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 3: Copy full transcript
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportDialog = false
                                viewModel.copyConversationToClipboard(context)
                            }
                            .testTag("export_copy_transcript_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryNeon.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "نسخ المحادثة بالكامل",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "نسخ كافة الرسائل كنص منسق إلى الحافظة",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false },
                    modifier = Modifier.testTag("close_export_dialog_button")
                ) {
                    Text("إغلاق", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showAudioPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAudioPermissionDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MicNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "إذن الميكروفون مطلوب للإملاء الصوتي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "يحتاج تطبيق نوفا AI إذن الوصول للميكروفون ليتمكن من تحويل كلامك إلى نصوص مكتوبة بدلاً من الكتابة اليدوية.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAudioPermissionDialog = false
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.testTag("grant_audio_permission_button")
                ) {
                    Text("منح الإذن الآن")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAudioPermissionDialog = false },
                    modifier = Modifier.testTag("dismiss_audio_permission_button")
                ) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showVoiceSheet) {
        VoiceDictationSheet(
            voiceState = voiceState,
            onStartListening = {
                voiceInputManager.startListening(
                    onFinalResult = { recognized ->
                        viewModel.appendVoiceInput(recognized)
                    }
                )
            },
            onStopListening = {
                voiceInputManager.stopListening()
            },
            onDismiss = {
                showVoiceSheet = false
                voiceInputManager.stopListening()
            },
            onInsertText = { text ->
                viewModel.appendVoiceInput(text)
                Toast.makeText(context, "تم إدراج النص الصوتي 🎙️", Toast.LENGTH_SHORT).show()
            },
            onSendDirectly = { text ->
                viewModel.appendVoiceInput(text)
                viewModel.sendChatMessage()
            },
            onLaunchSystemRecognizer = {
                try {
                    systemSpeechLauncher.launch(
                        VoiceInputManager.createRecognizerIntent(context)
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "اللاقط الصوتي غير متوفر على هذا الجهاز", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showVoiceAssistantMode) {
        VoiceAssistantModeDialog(
            voiceState = voiceState,
            ttsHelper = ttsHelper,
            isAiResponding = isGenerating,
            lastAiResponse = lastAiResponse,
            onStartListening = {
                voiceInputManager.startListening(
                    onFinalResult = { text ->
                        viewModel.sendChatMessage(text)
                    }
                )
            },
            onStopListening = {
                voiceInputManager.stopListening()
            },
            onSubmitVoiceQuery = { text ->
                viewModel.sendChatMessage(text)
            },
            onDismiss = {
                showVoiceAssistantMode = false
                voiceInputManager.stopListening()
                ttsHelper.stop()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Clean Minimal Header (Title & Neon Logo + Compact Voice Mode & Options)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding (Clean & Uncluttered)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryNeon, SecondaryCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Nova AI",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "نوفا Nova AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Compact Header Action Icons (Export & Clear)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("share_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "تصدير المحادثة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "مسح المحادثة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Messages List (Clean, wide, spacious without large distracting cards)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(chatMessages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    onCopy = { viewModel.copyTextToClipboard(context, message.text) },
                    onDelete = { viewModel.deleteChatMessage(message.id) },
                    onSaveImage = { url -> viewModel.saveImageFromChat(context, url) },
                    onShareVideo = { url -> viewModel.shareVideo(context, url, message.text) }
                )
            }

            if (isGenerating) {
                item {
                    AiGeneratingBubble()
                }
            }
        }

        // Sleek Compact Single-Horizontal Row Input Bar (Height ~50dp, Zero Card Bulk, Sticks to Bottom)
        Surface(
            color = Color(0xFF0C101A),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Subtle divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                )

                // Attached Image Preview Banner (Clear Thumbnail, Edit/Analyze badge & 'X' remove button)
                AnimatedVisibility(
                    visible = selectedBitmap != null || selectedUri != null
                ) {
                    Surface(
                        color = Color(0xFF131D33),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedBitmap != null) {
                                    Image(
                                        bitmap = selectedBitmap!!.asImageBitmap(),
                                        contentDescription = "صورة مرفقة",
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (selectedUri != null) {
                                    AsyncImage(
                                        model = selectedUri,
                                        contentDescription = "صورة مرفقة",
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "🎨 صورة مرفقة للتعديل أو التحليل",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryCyan
                                    )
                                    Text(
                                        text = "اكتب طلبك (مثلاً: تغيير الخلفية إلى غابة سحرية)",
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.clearSelectedImage() },
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("remove_attached_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إلغاء الصورة",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // Single Horizontal Row (Fixed 50dp, Camera, Gallery, Slim Center Input, Mic, Neon Send)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Attachment: Camera (Small Circle 34dp)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF161F33))
                            .clickable { cameraLauncher.launch(null) }
                            .testTag("chat_camera_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "كاميرا",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Attachment: Gallery / Photo Library (Small Circle 34dp)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF161F33))
                            .clickable {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("chat_attach_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "المعرض (إضافة صورة)",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Center: Slim Horizontal Input Box (Soft-rounded rectangle, Single line, Height 36dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141C2E))
                            .border(1.dp, Color(0xFF24324F), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = chatInput,
                            onValueChange = { viewModel.updateChatInput(it) },
                            singleLine = true,
                            maxLines = 1,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(PrimaryNeon),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_input"),
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (chatInput.isEmpty()) {
                                            Text(
                                                text = if (selectedBitmap != null || selectedUri != null)
                                                    "اكتب التعديل المطلوب (مثل: تغيير الخلفية...)"
                                                else
                                                    "اكتب رسالتك هنا...",
                                                color = Color(0xFF64748B),
                                                fontSize = 12.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                    if (chatInput.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "مسح",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.updateChatInput("") }
                                                .testTag("chat_clear_input_button")
                                        )
                                    }
                                }
                            }
                        )
                    }

                    // Microphone: Voice Recording (Small Circle 34dp)
                    val isMicActive = voiceState.isListening
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMicActive)
                                    Brush.linearGradient(listOf(ErrorRed, AccentPink))
                                else
                                    SolidColor(Color(0xFF161F33))
                            )
                            .clickable {
                                val isGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (isGranted) {
                                    viewModel.analyticsManager.trackVoiceDictationUsed(true)
                                    showVoiceSheet = true
                                    voiceInputManager.startListening(
                                        onFinalResult = { recognized ->
                                            viewModel.appendVoiceInput(recognized)
                                        }
                                    )
                                } else {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            .testTag("chat_voice_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMicActive) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "تسجيل صوتي",
                            tint = if (isMicActive) Color.White else PrimaryNeon,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Send Button: Neon Illuminated (Small Circle 34dp)
                    val isSendEnabled = (chatInput.isNotBlank() || selectedBitmap != null || selectedUri != null) && !isGenerating
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSendEnabled)
                                    Brush.linearGradient(listOf(PrimaryNeon, SecondaryCyan))
                                else
                                    SolidColor(Color(0xFF161F33))
                            )
                            .clickable(enabled = isSendEnabled) {
                                viewModel.sendChatMessage()
                            }
                            .testTag("chat_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = if (isSendEnabled) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onSaveImage: (String) -> Unit = {},
    onShareVideo: (String) -> Unit = {}
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) UserBubble else AiBubble
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryNeon, SecondaryCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(bubbleColor)
                    .border(
                        width = 1.dp,
                        color = if (isUser) Color.Transparent else CardBorderDark,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    // Attached / Generated Media rendering in message bubble (Images or Native Embedded Video Player)
                    val mediaUri = message.imageUri
                    if (!mediaUri.isNullOrBlank()) {
                        val isVideo = mediaUri.endsWith(".mp4", ignoreCase = true) ||
                                mediaUri.contains(".mp4") ||
                                mediaUri.contains("video", ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF131D33),
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column {
                                if (isVideo) {
                                    // Native Embedded Video Player with controls, autoPlay & loop
                                    EmbeddedChatVideoPlayer(videoUrl = mediaUri)

                                    if (!isUser) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🎬 مشغل فيديو سينمائي مدمج",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryCyan
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF1E293B))
                                                    .clickable { onShareVideo(mediaUri) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "مشاركة الفيديو",
                                                    tint = SecondaryCyan,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "مشاركة الفيديو",
                                                    fontSize = 10.sp,
                                                    color = SecondaryCyan
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    SubcomposeAsyncImage(
                                        model = mediaUri,
                                        contentDescription = "صورة في المحادثة",
                                        loading = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .background(SurfaceVariantDark),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(28.dp),
                                                        strokeWidth = 2.5.dp,
                                                        color = PrimaryNeon
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "جارٍ تحميل الصورة بجودة فائقة... 🎨",
                                                        color = TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        },
                                        error = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .background(SurfaceVariantDark),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "جاهزة للعرض 🖼️",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 260.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Action bar for newly generated/edited images
                                    if (!isUser) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "✨ تم التعديل بـ Nova AI",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = AccentEmerald
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF1E293B))
                                                    .clickable { onSaveImage(mediaUri) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "حفظ الصورة",
                                                    tint = SecondaryCyan,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "حفظ بالمعرض",
                                                    fontSize = 10.sp,
                                                    color = SecondaryCyan
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = message.text,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 10.sp,
                            color = if (isUser) Color.White.copy(alpha = 0.7f) else TextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ النص",
                            tint = if (isUser) Color.White.copy(alpha = 0.8f) else TextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onCopy() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "حذف الرسالة",
                            tint = if (isUser) Color.White.copy(alpha = 0.8f) else TextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiGeneratingBubble() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(PrimaryNeon, SecondaryCyan))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI يفكر",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = AiBubble,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = SecondaryCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جارٍ التفكير ومعالجة التعديل... 🎨⚡",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun EmbeddedChatVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    var isBuffering by remember(videoUrl) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
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
                            isBuffering = false
                        }
                    }
                }
            },
            update = { webView ->
                if (webView.tag != videoUrl) {
                    webView.tag = videoUrl
                    isBuffering = true
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
                          <video id="player" src="$videoUrl" controls autoplay loop playsinline preload="auto"></video>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL("https://nova.ai", htmlData, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = SecondaryCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "جارٍ تشغيل الفيديو المدمج... 🎬",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}


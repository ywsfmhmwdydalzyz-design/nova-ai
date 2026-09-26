package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.ChatMessageEntity
import com.example.data.GeneratedImageEntity
import com.example.data.GeneratedVideoEntity
import com.example.network.AiService
import com.example.data.AppPreferences
import com.example.data.PreferencesManager
import com.example.util.AnalyticsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AiStudioTab {
    CHAT,
    IMAGE,
    VIDEO
}

class AiStudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val preferencesManager = PreferencesManager(application)
    val analyticsManager = AnalyticsManager(application)

    val preferences: StateFlow<AppPreferences> = preferencesManager.preferences

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _cacheClearMessage = MutableStateFlow<String?>(null)
    val cacheClearMessage: StateFlow<String?> = _cacheClearMessage.asStateFlow()

    // Current active tab
    private val _selectedTab = MutableStateFlow(AiStudioTab.CHAT)
    val selectedTab: StateFlow<AiStudioTab> = _selectedTab.asStateFlow()

    // --- Chat State ---
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    // Multimodal image attachment in chat
    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _lastAiResponse = MutableStateFlow<String?>(null)
    val lastAiResponse: StateFlow<String?> = _lastAiResponse.asStateFlow()

    // --- Image Generation State ---
    private val _imagePrompt = MutableStateFlow("")
    val imagePrompt: StateFlow<String> = _imagePrompt.asStateFlow()

    private val _selectedImageStyle = MutableStateFlow("Realistic")
    val selectedImageStyle: StateFlow<String> = _selectedImageStyle.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow("1:1")
    val selectedAspectRatio: StateFlow<String> = _selectedAspectRatio.asStateFlow()

    private val _isImageGenerating = MutableStateFlow(false)
    val isImageGenerating: StateFlow<Boolean> = _isImageGenerating.asStateFlow()

    private val _isPromptEnhancing = MutableStateFlow(false)
    val isPromptEnhancing: StateFlow<Boolean> = _isPromptEnhancing.asStateFlow()

    private val _enhancedPrompt = MutableStateFlow<String?>(null)
    val enhancedPrompt: StateFlow<String?> = _enhancedPrompt.asStateFlow()

    private val _currentImageUrl = MutableStateFlow<String?>(null)
    val currentImageUrl: StateFlow<String?> = _currentImageUrl.asStateFlow()

    val savedImages: StateFlow<List<GeneratedImageEntity>> = repository.savedImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Video Generation State ---
    private val _videoPrompt = MutableStateFlow("")
    val videoPrompt: StateFlow<String> = _videoPrompt.asStateFlow()

    private val _videoDurationSec = MutableStateFlow(10)
    val videoDurationSec: StateFlow<Int> = _videoDurationSec.asStateFlow()

    private val _videoQuality = MutableStateFlow("1080p")
    val videoQuality: StateFlow<String> = _videoQuality.asStateFlow()

    private val _videoStyle = MutableStateFlow("Cinematic")
    val videoStyle: StateFlow<String> = _videoStyle.asStateFlow()

    private val _isVideoGenerating = MutableStateFlow(false)
    val isVideoGenerating: StateFlow<Boolean> = _isVideoGenerating.asStateFlow()

    private val _videoStage = MutableStateFlow("")
    val videoStage: StateFlow<String> = _videoStage.asStateFlow()

    private val _videoProgressPercentage = MutableStateFlow(0)
    val videoProgressPercentage: StateFlow<Int> = _videoProgressPercentage.asStateFlow()

    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()

    private val _currentVideoPosterUrl = MutableStateFlow<String?>(null)
    val currentVideoPosterUrl: StateFlow<String?> = _currentVideoPosterUrl.asStateFlow()

    val savedVideos: StateFlow<List<GeneratedVideoEntity>> = repository.savedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        analyticsManager.trackTabSelected(_selectedTab.value)
        // Seed initial welcoming message only on first launch when DB is genuinely empty
        viewModelScope.launch {
            if (repository.getChatMessageCount() == 0) {
                repository.addChatMessage(
                    isUser = false,
                    text = "مرحباً بك في نوفا (Nova AI)! 🤖✨\n\nأنا مساعدك الذكي المتكامل، جاهز لمساعدتك في كتابة النصوص، صياغة الأكواد، والإجابة عن أي استفسار.\n\nتاريخ محادثاتك محفوظ محلياً عبر قاعدة بيانات Room على جهازك تلقائياً."
                )
            }
            refreshCacheSize()
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSizeBytes.value = preferencesManager.calculateCacheSizeBytes()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val cleared = preferencesManager.clearCache()
            _cacheSizeBytes.value = preferencesManager.calculateCacheSizeBytes()
            val formatted = PreferencesManager.formatBytes(cleared)
            _cacheClearMessage.value = "تم تنظيف الذاكرة المؤقتة بنجاح ($formatted)"
            delay(3000)
            _cacheClearMessage.value = null
        }
    }

    fun clearAllChatHistory() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun setThemeMode(mode: String) {
        preferencesManager.setThemeMode(mode)
    }

    fun setAccentColor(color: String) {
        preferencesManager.setAccentColor(color)
    }

    fun setAiCreativity(creativity: String) {
        preferencesManager.setAiCreativity(creativity)
    }

    fun setDefaultAspectRatio(ratio: String) {
        preferencesManager.setDefaultAspectRatio(ratio)
        _selectedAspectRatio.value = ratio
    }

    fun setDefaultVideoQuality(quality: String) {
        preferencesManager.setDefaultVideoQuality(quality)
        _videoQuality.value = quality
    }

    fun resetPreferencesToDefault() {
        preferencesManager.resetToDefaults()
    }

    fun deleteChatMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteChatMessage(id)
        }
    }

    fun selectTab(tab: AiStudioTab) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            analyticsManager.trackTabSelected(tab)
        }
    }

    // --- Chat Actions ---
    fun updateChatInput(text: String) {
        _chatInput.value = text
    }

    fun setSelectedImage(bitmap: Bitmap?, uri: Uri?) {
        _selectedImageBitmap.value = bitmap
        _selectedImageUri.value = uri
    }

    fun clearSelectedImage() {
        _selectedImageBitmap.value = null
        _selectedImageUri.value = null
    }

    fun appendVoiceInput(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (_chatInput.value.isBlank()) {
            _chatInput.value = trimmed
        } else {
            _chatInput.value = "${_chatInput.value.trim()} $trimmed"
        }
    }

    fun sendChatMessage(presetPrompt: String? = null) {
        val query = presetPrompt ?: _chatInput.value.trim()
        val attachedBitmap = _selectedImageBitmap.value
        val attachedUri = _selectedImageUri.value

        if (query.isEmpty() && attachedBitmap == null) return
        if (_isChatGenerating.value) return

        analyticsManager.trackChatMessageSent(
            messageLength = query.length,
            hasImage = attachedBitmap != null || attachedUri != null,
            isVoice = presetPrompt != null
        )

        if (presetPrompt == null) {
            _chatInput.value = ""
            clearSelectedImage()
        }

        val isEditRequest = (attachedBitmap != null || attachedUri != null) && isImageEditRequest(query)
        val isVideoReq = (attachedBitmap == null && attachedUri == null) && isVideoRequest(query)
        val isImageGenReq = (attachedBitmap == null && attachedUri == null) && isImageGenerationRequest(query)

        viewModelScope.launch {
            repository.addChatMessage(
                isUser = true,
                text = query.ifBlank { if (isEditRequest) "🎨 طلب تعديل الصورة" else "📸 صورة مرفقة للتحليل الذكي" },
                imageUri = attachedUri?.toString()
            )
            _isChatGenerating.value = true

            if (isEditRequest) {
                val editResult = AiService.editImageInChat(query, attachedBitmap)
                _lastAiResponse.value = editResult.explanation
                repository.addChatMessage(
                    isUser = false,
                    text = editResult.explanation,
                    imageUri = editResult.editedImageUrl
                )
            } else if (isVideoReq) {
                val resolved = AiService.resolveVideoForPrompt(query, "Cinematic", 10)
                repository.addGeneratedVideo(
                    prompt = query,
                    style = "Cinematic",
                    durationSec = 10,
                    quality = "1080p Full HD",
                    videoUrl = resolved.videoUrl
                )
                val replyText = "تم إنتاج وتوليد الفيديو بنجاح! 🎬✨\nإليك المشهد السينمائي جاهز للمشاهدة المباشرة مع التحكم بالصوت والتشغيل أدناه:"
                _lastAiResponse.value = replyText
                repository.addChatMessage(
                    isUser = false,
                    text = replyText,
                    imageUri = resolved.videoUrl
                )
            } else if (isImageGenReq) {
                val enhancedPrompt = AiService.enhanceImagePrompt(query)
                val imageUrl = AiService.generateImage(enhancedPrompt, "realistic", "1:1")
                repository.addGeneratedImage(
                    prompt = query,
                    style = "realistic",
                    imageUrl = imageUrl
                )
                val replyText = "تم توليد الصورة بنجاح! 🎨✨\nإليك العمل الفني فائق الدقة بناءً على طلبك:"
                _lastAiResponse.value = replyText
                repository.addChatMessage(
                    isUser = false,
                    text = replyText,
                    imageUri = imageUrl
                )
            } else {
                val history = chatMessages.value.map { Pair(it.isUser, it.text) }
                val response = AiService.sendMultimodalChatMessage(
                    userPrompt = query,
                    imageBitmap = attachedBitmap,
                    history = history
                )

                _lastAiResponse.value = response
                repository.addChatMessage(isUser = false, text = response)
            }
            _isChatGenerating.value = false
        }
    }

    private fun isVideoRequest(prompt: String): Boolean {
        val p = prompt.lowercase().trim()
        if (p.isEmpty()) return false
        val videoKeywords = listOf(
            "فيديو", "فديو", "مقطع", "مشهد متحرك", "أنشئ فيديو", "ولد فيديو", "اصنع فيديو", "اعمل فيديو",
            "video", "generate video", "make video", "create video", "motion"
        )
        return videoKeywords.any { p.contains(it) }
    }

    private fun isImageGenerationRequest(prompt: String): Boolean {
        val p = prompt.lowercase().trim()
        if (p.isEmpty()) return false
        val imageKeywords = listOf(
            "ارسم", "رسمة", "توليد صورة", "صمم صورة", "ولد صورة", "اصنع صورة", "اعمل صورة",
            "draw", "generate image", "create image", "paint", "لوحة"
        )
        return imageKeywords.any { p.contains(it) }
    }

    private fun isImageEditRequest(prompt: String): Boolean {
        val p = prompt.lowercase().trim()
        if (p.isEmpty()) return false
        val editKeywords = listOf(
            "تعديل", "تغيير", "غير", "بدل", "استبدل", "أضف", "اضف", "حط", "ركب",
            "احذف", "شيل", "إزالة", "خلفية", "لون", "اجعل", "حول", "لبس",
            "نظارة", "قبعة", "شعر", "عين", "ملابس", "ستايل", "رسم", "انيمي",
            "edit", "change", "modify", "replace", "add", "remove", "inpainting",
            "background", "sunglasses", "filter", "transform", "make it", "swap"
        )
        return editKeywords.any { p.contains(it) }
    }

    fun saveImageFromChat(context: Context, imageUrl: String) {
        viewModelScope.launch {
            val uri = AiService.saveImageToGallery(context, imageUrl, "Nova_Edit")
            if (uri != null) {
                Toast.makeText(context, "تم حفظ الصورة المعدلة في المعرض بنجاح 🖼️", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "تعذر حفظ الصورة، يرجى المحاولة لاحقاً", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearChat() {
        analyticsManager.trackChatCleared()
        viewModelScope.launch {
            repository.clearChat()
            repository.addChatMessage(
                isUser = false,
                text = "تم مسح المحادثة بنجاح 🧹✨\nكيف يمكنني مساعدتك الآن؟"
            )
        }
    }

    fun copyTextToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AI Studio Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ النص إلى الحافظة", Toast.LENGTH_SHORT).show()
    }

    fun buildConversationText(messages: List<ChatMessageEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val msgTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append("🤖 نوفا (Nova AI) - سجل تصدير المحادثة\n")
        sb.append("تاريخ التصدير: $currentDate\n")
        sb.append("إجمالي الرسائل: ${messages.size} رسالة\n")
        sb.append("==================================================\n\n")

        messages.forEachIndexed { index, msg ->
            val sender = if (msg.isUser) "👤 المستخدم" else "🤖 نوفا AI"
            val time = msgTimeFormat.format(Date(msg.timestamp))
            sb.append("[${index + 1}] $sender ($time):\n")
            sb.append(msg.text.trim())
            sb.append("\n\n--------------------------------------------------\n\n")
        }

        sb.append("==================================================\n")
        sb.append("تم تصدير هذا السجل عبر تطبيق نوفا (Nova AI Studio)\n")
        sb.append("كافة الحقوق محفوظة © 2026\n")
        sb.append("==================================================\n")

        return sb.toString()
    }

    fun shareConversationFile(context: Context) {
        val messages = chatMessages.value
        if (messages.isEmpty()) {
            Toast.makeText(context, "لا توجد رسائل لتصديرها", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "nova_chat_$timeStamp.txt"
            val file = File(exportDir, fileName)

            file.writeText(buildConversationText(messages))

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "سجل محادثة نوفا AI")
                putExtra(Intent.EXTRA_TEXT, "مرفق ملف نصي (.txt) يحتوي على سجل محادثتك من تطبيق نوفا AI.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة سجل المحادثة كملف نصي")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            analyticsManager.trackChatExported("share_txt", chatMessages.value.size)
        } catch (e: Exception) {
            Toast.makeText(context, "حدث خطأ أثناء تصدير الملف: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun saveConversationToUri(context: Context, uri: Uri) {
        try {
            val content = buildConversationText(chatMessages.value)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "تم حفظ سجل المحادثة بنجاح 📁", Toast.LENGTH_SHORT).show()
            analyticsManager.trackChatExported("save_txt", chatMessages.value.size)
        } catch (e: Exception) {
            Toast.makeText(context, "فشل حفظ الملف: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun copyConversationToClipboard(context: Context) {
        val messages = chatMessages.value
        if (messages.isEmpty()) {
            Toast.makeText(context, "لا توجد رسائل لنسخها", Toast.LENGTH_SHORT).show()
            return
        }
        val content = buildConversationText(messages)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Nova AI Chat History", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ سجل المحادثة بالكامل إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
        analyticsManager.trackChatExported("clipboard", messages.size)
    }

    // --- Image Actions ---
    fun updateImagePrompt(text: String) {
        _imagePrompt.value = text
    }

    fun selectImageStyle(style: String) {
        _selectedImageStyle.value = style
    }

    fun selectAspectRatio(ratio: String) {
        _selectedAspectRatio.value = ratio
    }

    fun setRandomImagePrompt() {
        val prompts = listOf(
            "رائد فضاء عربي يستكشف واحة فضائية مستقبلية على المريخ بإضاءة نيون",
            "صقر ذهبي أسطوري يحلق فوق مدينة نيوم المستقبلية في وقت الغروب",
            "مدينة ذكية متطورة وسط السحاب مع قطارات مغناطيسية طائرة",
            "مختبر ذكاء اصطناعي فائق التطور مع روبوت يساعد عالماً في ابتكار طاقة متجددة",
            "واحة خضراء مذهلة في أعماق الصحراء مع مياه نقية متلألئة تحت أضواء النجوم"
        )
        _imagePrompt.value = prompts.random()
    }

    fun enhanceImagePrompt() {
        val prompt = _imagePrompt.value.trim()
        if (prompt.isEmpty() || _isPromptEnhancing.value) return

        analyticsManager.trackImagePromptEnhanced()
        viewModelScope.launch {
            _isPromptEnhancing.value = true
            val enhanced = AiService.enhanceImagePrompt(prompt)
            _enhancedPrompt.value = enhanced
            _imagePrompt.value = enhanced
            _isPromptEnhancing.value = false
        }
    }

    fun generateImage() {
        val prompt = _imagePrompt.value.trim()
        if (prompt.isEmpty() || _isImageGenerating.value) return

        analyticsManager.trackImageGenerated(
            promptLength = prompt.length,
            style = _selectedImageStyle.value,
            aspectRatio = _selectedAspectRatio.value,
            isEnhanced = _enhancedPrompt.value != null
        )

        viewModelScope.launch {
            _isImageGenerating.value = true
            _currentImageUrl.value = null

            // Auto enhance prompt if in Arabic or short
            val targetPrompt = if (prompt.any { it in '\u0600'..'\u06FF' } || prompt.split(" ").size < 6) {
                val enhanced = AiService.enhanceImagePrompt(prompt)
                _enhancedPrompt.value = enhanced
                enhanced
            } else {
                prompt
            }

            // Build image URL via Pollinations AI (hyper-quality Flux model)
            val url = AiService.buildImageUrl(targetPrompt, _selectedImageStyle.value, _selectedAspectRatio.value)
            delay(1200) // Aesthetic delay for animation
            _currentImageUrl.value = url

            // Save to Room DB
            repository.saveImage(
                prompt = prompt,
                style = _selectedImageStyle.value,
                imageUrl = url
            )

            _isImageGenerating.value = false
        }
    }

    fun saveImageToDevice(context: Context, url: String) {
        analyticsManager.trackImageSaved()
        viewModelScope.launch {
            Toast.makeText(context, "جارٍ حفظ الصورة في الاستوديو...", Toast.LENGTH_SHORT).show()
            val uri = AiService.saveImageToGallery(context, url)
            if (uri != null) {
                Toast.makeText(context, "تم حفظ الصورة بنجاح في مجلد Pictures/Nova_AI! 📸", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "تم نسخ رابط الصورة بجودة عالية", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareImage(context: Context, url: String, prompt: String) {
        analyticsManager.trackImageShared()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "صورة مولدة بالذكاء الاصطناعي - AI Studio")
            putExtra(Intent.EXTRA_TEXT, "شاهد هذه الصورة الرائعة المولدة بواسطة AI Studio:\nالوصف: $prompt\nالرابط: $url")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الصورة عبر"))
    }

    // --- Video Actions ---
    fun updateVideoPrompt(text: String) {
        _videoPrompt.value = text
    }

    fun selectVideoDuration(seconds: Int) {
        _videoDurationSec.value = seconds
    }

    fun selectVideoQuality(quality: String) {
        _videoQuality.value = quality
    }

    fun selectVideoStyle(style: String) {
        _videoStyle.value = style
    }

    fun setRandomVideoPrompt() {
        val prompts = listOf(
            "طيران بطيء بكاميرا سينمائية درون فوق مدينة سايبر بانك مضيئة بالأمطار والنيون",
            "سفينة فضاء تخرج من ثقب دودي فلكي محاط بهالات ضوئية فائقة السرعة",
            "شلال متوهج ببلورات زرقاء في غابة استوائية ساحرة في ليل مضيء",
            "سيارة رياضية فائقة السرعة تنطلق بسرعة البرق على طريق مستقبلي متعرج"
        )
        _videoPrompt.value = prompts.random()
    }

    fun loadSavedVideo(url: String, prompt: String, style: String, duration: Int) {
        _videoPrompt.value = prompt
        _videoStyle.value = style
        _videoDurationSec.value = duration
        val resolved = AiService.resolveVideoForPrompt(prompt, style, duration)
        _currentVideoPosterUrl.value = resolved.posterThumbnailUrl
        _currentVideoUrl.value = url
        analyticsManager.trackVideoPlayed(duration)
    }

    fun generateVideo() {
        val prompt = _videoPrompt.value.trim()
        if (prompt.isEmpty() || _isVideoGenerating.value) return

        val duration = _videoDurationSec.value
        analyticsManager.trackVideoGenerated(
            promptLength = prompt.length,
            style = _videoStyle.value,
            durationSeconds = duration,
            quality = _videoQuality.value
        )

        viewModelScope.launch {
            _isVideoGenerating.value = true
            _currentVideoUrl.value = null
            _videoProgressPercentage.value = 4

            val duration = _videoDurationSec.value
            val stages = listOf(
                Pair(15, "تحليل المشهد وتفكيك السيناريو الإبداعي... 📝"),
                Pair(35, "توليد الإطارات المفتاحية بدقة ${_videoQuality.value}... 🎨"),
                Pair(65, "محاكاة الإضاءة والحركة الفيزيائية (${duration} ثوانٍ)... ⚡"),
                Pair(90, "معالجة الريندر السينمائي النهائي وتجميع الإطارات... 🎬"),
                Pair(100, "اكتمل التوليد بنجاح! 🚀")
            )

            for ((targetProgress, stage) in stages) {
                _videoStage.value = stage
                val currentP = _videoProgressPercentage.value
                val stepCount = (targetProgress - currentP).coerceAtLeast(1)
                val delayPerStep = (600L / stepCount).coerceIn(15L, 80L)
                for (p in (currentP + 1)..targetProgress) {
                    _videoProgressPercentage.value = p
                    delay(delayPerStep)
                }
            }

            val resolved = AiService.resolveVideoForPrompt(prompt, _videoStyle.value, duration)
            _currentVideoPosterUrl.value = resolved.posterThumbnailUrl
            _currentVideoUrl.value = resolved.videoUrl

            repository.saveVideo(
                prompt = prompt,
                style = _videoStyle.value,
                durationSec = duration,
                quality = _videoQuality.value,
                videoUrl = resolved.videoUrl
            )

            _isVideoGenerating.value = false
        }
    }

    fun shareVideo(context: Context, url: String, prompt: String) {
        analyticsManager.trackVideoShared()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "فيديو مولد بالذكاء الاصطناعي - AI Studio")
            putExtra(Intent.EXTRA_TEXT, "شاهد هذا الفيديو السينمائي المولد بواسطة AI Studio:\nالمشهد: $prompt\nرابط العرض: $url")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الفيديو عبر"))
    }
}

package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AppRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.appDao()

    val chatMessages: Flow<List<ChatMessageEntity>> = dao.getAllChatMessages()
    val savedImages: Flow<List<GeneratedImageEntity>> = dao.getAllImages()
    val savedVideos: Flow<List<GeneratedVideoEntity>> = dao.getAllVideos()

    suspend fun getChatMessageCount(): Int {
        return dao.getChatMessageCount()
    }

    suspend fun addChatMessage(isUser: Boolean, text: String, imageUri: String? = null): Long {
        return dao.insertChatMessage(ChatMessageEntity(isUser = isUser, text = text, imageUri = imageUri))
    }

    suspend fun deleteChatMessage(id: Long) {
        dao.deleteChatMessageById(id)
    }

    suspend fun clearChat() {
        dao.clearAllChatMessages()
    }

    suspend fun saveImage(prompt: String, style: String, imageUrl: String): Long {
        return dao.insertImage(GeneratedImageEntity(prompt = prompt, style = style, imageUrl = imageUrl))
    }

    suspend fun saveVideo(prompt: String, style: String, durationSec: Int, quality: String, videoUrl: String): Long {
        return dao.insertVideo(
            GeneratedVideoEntity(
                prompt = prompt,
                style = style,
                durationSec = durationSec,
                quality = quality,
                videoUrl = videoUrl
            )
        )
    }
}

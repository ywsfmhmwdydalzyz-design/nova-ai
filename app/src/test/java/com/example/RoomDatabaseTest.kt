package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDao
import com.example.data.AppDatabase
import com.example.data.ChatMessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: AppDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.appDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveChatMessages() = runBlocking {
        // Given
        val userMsg = ChatMessageEntity(isUser = true, text = "كيف يعمل الذكاء الاصطناعي؟")
        val aiMsg = ChatMessageEntity(isUser = false, text = "يعتمد على الشبكات العصبية العميقة والخوارزميات التعلم الآلي.")

        // When
        dao.insertChatMessage(userMsg)
        dao.insertChatMessage(aiMsg)

        // Then
        val count = dao.getChatMessageCount()
        assertEquals(2, count)

        val messages = dao.getAllChatMessages().first()
        assertEquals(2, messages.size)
        assertEquals("كيف يعمل الذكاء الاصطناعي؟", messages[0].text)
        assertTrue(messages[0].isUser)
        assertEquals("يعتمد على الشبكات العصبية العميقة والخوارزميات التعلم الآلي.", messages[1].text)
    }

    @Test
    fun testDeleteAndClearChatMessages() = runBlocking {
        // Given
        val msgId = dao.insertChatMessage(ChatMessageEntity(isUser = true, text = "رسالة للاختبار"))
        assertEquals(1, dao.getChatMessageCount())

        // When
        dao.deleteChatMessageById(msgId)

        // Then
        assertEquals(0, dao.getChatMessageCount())
        val messages = dao.getAllChatMessages().first()
        assertTrue(messages.isEmpty())
    }
}

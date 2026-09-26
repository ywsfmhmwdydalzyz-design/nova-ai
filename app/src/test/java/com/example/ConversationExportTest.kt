package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChatMessageEntity
import com.example.ui.AiStudioViewModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConversationExportTest {

    private lateinit var viewModel: AiStudioViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = AiStudioViewModel(context as android.app.Application)
    }

    @Test
    fun testBuildConversationText() {
        val messages = listOf(
            ChatMessageEntity(id = 1, isUser = true, text = "ما هو الذكاء الاصطناعي؟", timestamp = 1727250000000L),
            ChatMessageEntity(id = 2, isUser = false, text = "الذكاء الاصطناعي هو محاكاة للذكاء البشري.", timestamp = 1727250010000L)
        )

        val exportedText = viewModel.buildConversationText(messages)

        assertNotNull(exportedText)
        assertTrue("Contains title", exportedText.contains("نوفا (Nova AI)"))
        assertTrue("Contains user message", exportedText.contains("ما هو الذكاء الاصطناعي؟"))
        assertTrue("Contains AI response", exportedText.contains("الذكاء الاصطناعي هو محاكاة للذكاء البشري."))
        assertTrue("Contains user label", exportedText.contains("المستخدم"))
        assertTrue("Contains Nova AI label", exportedText.contains("نوفا AI"))
        assertTrue("Contains message count", exportedText.contains("إجمالي الرسائل: 2"))
    }

    @Test
    fun testExportDirectoryCreation() {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        assertTrue("Export directory must exist", exportDir.exists() && exportDir.isDirectory)

        val testExportFile = File(exportDir, "test_chat.txt")
        testExportFile.writeText("سجل تجريبي")
        assertTrue("Export file written", testExportFile.exists())
        assertTrue("File content matches", testExportFile.readText() == "سجل تجريبي")
    }
}

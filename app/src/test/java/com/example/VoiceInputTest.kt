package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AiStudioViewModel
import com.example.util.VoiceInputManager
import com.example.util.VoiceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceInputTest {

    private lateinit var viewModel: AiStudioViewModel
    private lateinit var context: Context
    private lateinit var voiceInputManager: VoiceInputManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = AiStudioViewModel(context as android.app.Application)
        voiceInputManager = VoiceInputManager(context)
    }

    @Test
    fun testAppendVoiceInput_emptyInitial() {
        viewModel.updateChatInput("")
        viewModel.appendVoiceInput("ما هي أحدث أخبار التكنولوجيا؟")

        assertEquals("ما هي أحدث أخبار التكنولوجيا؟", viewModel.chatInput.value)
    }

    @Test
    fun testAppendVoiceInput_existingText() {
        viewModel.updateChatInput("مرحباً")
        viewModel.appendVoiceInput("كيف أتعلم البرمجة؟")

        assertEquals("مرحباً كيف أتعلم البرمجة؟", viewModel.chatInput.value)
    }

    @Test
    fun testVoiceStateDefaults() {
        val state = voiceInputManager.voiceState.value
        assertFalse(state.isListening)
        assertEquals("", state.text)
        assertEquals("", state.partialText)
        assertEquals(0f, state.soundLevel, 0.001f)
    }

    @Test
    fun testVoiceRecognizerIntentCreation() {
        val intent = VoiceInputManager.createRecognizerIntent(context, "تحدث الآن...")
        assertNotNull(intent)
        assertNotNull(intent.action)
    }
}

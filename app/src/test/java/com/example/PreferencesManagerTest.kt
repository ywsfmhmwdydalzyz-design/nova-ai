package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
class PreferencesManagerTest {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        preferencesManager = PreferencesManager(context)
        preferencesManager.resetToDefaults()
    }

    @Test
    fun testDefaultPreferences() {
        val prefs = preferencesManager.preferences.value
        assertEquals("dark", prefs.themeMode)
        assertEquals("cyan", prefs.accentColor)
        assertEquals("balanced", prefs.aiCreativity)
        assertEquals("1:1", prefs.defaultAspectRatio)
        assertEquals("1080p", prefs.defaultVideoQuality)
    }

    @Test
    fun testThemeAndAccentUpdates() {
        preferencesManager.setThemeMode("light")
        assertEquals("light", preferencesManager.preferences.value.themeMode)

        preferencesManager.setAccentColor("emerald")
        assertEquals("emerald", preferencesManager.preferences.value.accentColor)

        preferencesManager.setAiCreativity("creative")
        assertEquals("creative", preferencesManager.preferences.value.aiCreativity)

        preferencesManager.setDefaultAspectRatio("16:9")
        assertEquals("16:9", preferencesManager.preferences.value.defaultAspectRatio)

        preferencesManager.setDefaultVideoQuality("4K")
        assertEquals("4K", preferencesManager.preferences.value.defaultVideoQuality)
    }

    @Test
    fun testCacheCalculationAndClearing() = runBlocking {
        // Create dummy cache file
        val testFile = File(context.cacheDir, "test_cache.tmp")
        testFile.writeText("sample cache content")

        val sizeBefore = preferencesManager.calculateCacheSizeBytes()
        assertTrue("Cache size should be greater than 0", sizeBefore > 0)

        // Clear cache
        val cleared = preferencesManager.clearCache()
        assertTrue("Cleared bytes should be greater than 0", cleared > 0)

        val formatted = PreferencesManager.formatBytes(cleared)
        assertNotNull(formatted)
        assertTrue(formatted.contains("بايت") || formatted.contains("كيلوبايت") || formatted.contains("ميغابايت"))
    }
}

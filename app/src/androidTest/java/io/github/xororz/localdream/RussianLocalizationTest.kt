package io.github.xororz.localdream

import android.app.KeyguardManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.xororz.localdream.data.LanguagePreferences
import io.github.xororz.localdream.data.localizedString
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RussianLocalizationResourcesTest {
    private fun context(language: String): Context {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(config)
    }

    @Test
    fun russianPluralFormsAndPlaceholders() {
        val resources = context("ru").resources
        for ((count, expected) in listOf(1 to "файл", 2 to "файла", 5 to "файлов", 11 to "файлов", 21 to "файл", 22 to "файла", 25 to "файлов")) {
            assertEquals("$count $expected", resources.getQuantityString(R.plurals.file_count, count, count))
        }
        assertEquals("словарь · 22 записи", resources.getQuantityString(R.plurals.tag_imported_status, 22, "словарь", 22))
        assertEquals("Размер изображения: 512×768", resources.getString(R.string.image_size, 512, 768))
        assertEquals("Прогресс: 42%", resources.getString(R.string.generation_progress, 42))
    }

    @Test
    fun removedLanguagesFallBackToEnglish() {
        for (language in listOf("en", "ja", "ko", "zh", "de")) {
            assertEquals("Settings", context(language).getString(R.string.settings))
        }
        assertEquals("Настройки", context("ru").getString(R.string.settings))
    }
}

@RunWith(AndroidJUnit4::class)
class LanguageSettingsTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val originalLanguage = LanguagePreferences.current(context)

    @Before
    fun requireUnlockedDevice() {
        assumeFalse("Unlock the device to test the language settings UI", context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
    }

    @After
    fun restoreLanguage() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            LanguagePreferences.select(context, originalLanguage)
        }
    }

    @Test
    fun switchBothLanguagesAndKeepSettingsAfterRecreation() {
        compose.waitUntil(30_000) {
            listOf(R.string.available_models, R.string.got_it).any { id ->
                compose.onAllNodesWithText(compose.activity.getString(id))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            }
        }
        val gotIt = compose.activity.getString(R.string.got_it)
        if (compose.onAllNodesWithText(gotIt).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText(gotIt).performClick()
        }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.more_options)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.settings)).performClick()
        for ((language, title) in listOf("ru" to "Язык", "en" to "Language")) {
            val label = if (language == "ru") "Русский" else "English"
            compose.onNodeWithTag("settings_list").performScrollToNode(hasText(label))
            compose.onNodeWithText(label).performClick()
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText(title).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            }
            compose.onNodeWithText(title).assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { screenshot ->
                context.openFileOutput("localization_$language.png", Context.MODE_PRIVATE).use { output ->
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
                }
                screenshot.recycle()
            }
            assertEquals(language, LanguagePreferences.current(context))
            assertEquals(if (language == "ru") "Прогресс: 42%" else "Progress: 42%", context.localizedString(R.string.generation_progress, 42))
            compose.activityRule.scenario.recreate()
            compose.onNodeWithTag("settings_list").performScrollToNode(hasText(label))
            compose.onNodeWithText(title).assertIsDisplayed()
            assertEquals(language, compose.activity.resources.configuration.locales[0].language)
        }
    }
}

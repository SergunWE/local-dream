package io.github.xororz.localdream.data

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguagePreferences {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "language"

    fun supportedLanguage(language: String): String = if (language == "ru") "ru" else "en"

    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!locales.isEmpty) return supportedLanguage(locales[0].language)
        }
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        if (saved != null) return supportedLanguage(saved)
        return supportedLanguage(Resources.getSystem().configuration.locales[0].language)
    }

    // Supply the stored locale before Activity.onCreate on Android 9–12.
    // Android 13+ persists the choice through the platform LocaleManager.
    fun initialize(context: Context) {
        select(context, current(context))
    }

    fun select(context: Context, language: String) {
        val tag = supportedLanguage(language)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_LANGUAGE, tag)
            }
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Migrate an Android 9–12 preference after an OS upgrade. The
            // platform becomes the source of truth, including system edits.
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                remove(KEY_LANGUAGE)
            }
        }
    }

    // Services and repositories use application contexts, which AppCompat
    // does not localize on Android 12 and below. Resolve strings explicitly
    // so running services also pick up a language change.
    fun localizedContext(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(current(context)))
        return context.createConfigurationContext(configuration)
    }
}

fun Context.localizedString(@StringRes id: Int, vararg args: Any): String = LanguagePreferences.localizedContext(this).getString(id, *args)

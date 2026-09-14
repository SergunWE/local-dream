package io.github.xororz.localdream.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.xororz.localdream.R

private val millisecondsPattern = Regex("(\\d+)ms")
private val secondsPattern = Regex("(\\d+(?:\\.\\d+)?)s")
private val minutesSecondsPattern = Regex("(\\d+)m(\\d+)s")

// History and shared parameters retain the original language-independent
// duration (e.g. "1m5s"). Translate only when displaying it.
@Composable
internal fun localizedDuration(duration: String?): String {
    if (duration.isNullOrBlank() || duration == "unknown") return stringResource(R.string.time_unknown)
    millisecondsPattern.matchEntire(duration)?.let {
        val milliseconds = it.groupValues[1].toLongOrNull()
        if (milliseconds != null) return stringResource(R.string.time_milliseconds, milliseconds)
    }
    secondsPattern.matchEntire(duration)?.let {
        val seconds = it.groupValues[1].toDoubleOrNull()
        if (seconds != null && seconds.isFinite()) return stringResource(R.string.time_seconds, seconds)
    }
    minutesSecondsPattern.matchEntire(duration)?.let {
        val minutes = it.groupValues[1].toLongOrNull()
        val seconds = it.groupValues[2].toLongOrNull()
        if (minutes != null && seconds != null) return stringResource(R.string.time_minutes_seconds, minutes, seconds)
    }
    return duration
}

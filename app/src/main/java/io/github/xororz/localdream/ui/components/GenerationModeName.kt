package io.github.xororz.localdream.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.xororz.localdream.R
import io.github.xororz.localdream.data.GenerationMode

@Composable
internal fun generationModeName(mode: GenerationMode): String = stringResource(
    when (mode) {
        GenerationMode.TXT2IMG -> R.string.mode_txt2img
        GenerationMode.IMG2IMG -> R.string.mode_img2img
        GenerationMode.INPAINT -> R.string.mode_inpaint
        GenerationMode.ULTRAFIX -> R.string.ultrafix
        GenerationMode.UNKNOWN -> R.string.time_unknown
    },
)

package com.voicerecorder.presentation.ui.recorder.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voicerecorder.presentation.theme.FinalTalkTheme

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barWidth: Dp = 4.dp,
    spacing: Dp = 3.dp,
) {
    val primaryGradient = FinalTalkTheme.gradients.primaryGradient
    val fallbackColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(180.dp),
    ) {
        val barWidthPx = barWidth.toPx()
        val spacingPx = spacing.toPx()
        val stepPx = barWidthPx + spacingPx

        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Calculate max bars we can fit on screen
        val maxBars = (width / stepPx).toInt()
        val drawAmplitudes = amplitudes.takeLast(maxBars)

        if (drawAmplitudes.isEmpty()) {
            // Draw a flat center line when idle with a subtle neon glow
            drawRoundRect(
                color = fallbackColor.copy(alpha = 0.25f),
                topLeft = Offset(0f, centerY - 2.dp.toPx()),
                size = Size(width, 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
        } else {
            drawAmplitudes.forEachIndexed { index, amplitude ->
                // Draw from right to left to create scrolling effect
                val x = width - (drawAmplitudes.size - index) * stepPx + spacingPx / 2f
                // Scale bar height to take up max 85% of canvas height
                val barHeight = (amplitude * (height * 0.85f)).coerceAtLeast(6.dp.toPx())

                // Draw secondary neon glow behind active bars
                drawRoundRect(
                    color = fallbackColor.copy(alpha = 0.08f),
                    topLeft = Offset(x - barWidthPx / 2f, centerY - (barHeight + 12.dp.toPx()) / 2f),
                    size = Size(barWidthPx * 2f, barHeight + 12.dp.toPx()),
                    cornerRadius = CornerRadius(barWidthPx, barWidthPx),
                )

                // Main bar
                drawRoundRect(
                    brush = primaryGradient,
                    topLeft = Offset(x, centerY - barHeight / 2f),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f),
                )
            }
        }
    }
}

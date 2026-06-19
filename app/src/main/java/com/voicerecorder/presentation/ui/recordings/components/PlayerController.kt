package com.voicerecorder.presentation.ui.recordings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voicerecorder.domain.model.AudioRecording
import com.voicerecorder.domain.model.PlayerState
import com.voicerecorder.presentation.ui.util.FormatUtils
import com.voicerecorder.presentation.ui.util.glassmorphic
import com.voicerecorder.presentation.ui.util.gyroParallax
import com.voicerecorder.presentation.ui.util.neonAura
import com.voicerecorder.presentation.ui.util.weightlessDrift

@Composable
fun PlayerController(
    playerState: PlayerState,
    activeRecording: AudioRecording?,
    onPlayPauseToggle: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = playerState !is PlayerState.Idle && activeRecording != null

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        if (activeRecording != null) {
            val progressMs =
                when (playerState) {
                    is PlayerState.Playing -> playerState.progressMs
                    is PlayerState.Paused -> playerState.progressMs
                    else -> 0L
                }
            val durationMs =
                when (playerState) {
                    is PlayerState.Playing -> playerState.durationMs
                    is PlayerState.Paused -> playerState.durationMs
                    else -> activeRecording.durationMs
                }
            val isPlaying = playerState is PlayerState.Playing

            // To prevent slider jumping during drag, maintain user seeking status local state
            var isUserSeeking by remember { mutableStateOf(false) }
            var localSliderValue by remember { mutableFloatStateOf(0f) }

            val sliderValue =
                if (isUserSeeking) {
                    localSliderValue
                } else {
                    if (durationMs > 0) progressMs.toFloat() / durationMs.toFloat() else 0f
                }

            // Glassmorphic Floating Spaceship Cockpit Container with unified neon glow and motion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .weightlessDrift(durationMs = 4200, minDrift = -2.5f, maxDrift = 2.5f)
                    .gyroParallax()
                    .neonAura(color = MaterialTheme.colorScheme.primary, alpha = 0.18f, radiusFraction = 0.9f)
                    .glassmorphic(RoundedCornerShape(28.dp), borderWidth = 1.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header title & close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeRecording.title,
                                    style =
                                        MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            IconButton(onClick = onStop) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                            }
                        }

                        // Timeline Slider
                        Slider(
                            value = sliderValue.coerceIn(0f, 1f),
                            onValueChange = {
                                isUserSeeking = true
                                localSliderValue = it
                            },
                            onValueChangeFinished = {
                                isUserSeeking = false
                                val seekPos = (localSliderValue * durationMs).toLong()
                                onSeek(seekPos)
                            },
                            colors =
                                SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                ),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                        )

                        // Duration labels & controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Current time label
                            val elapsedText =
                                FormatUtils.formatDuration(
                                    if (isUserSeeking) (localSliderValue * durationMs).toLong() else progressMs,
                                )
                            Text(
                                text = elapsedText,
                                style =
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )

                            // Center Play/Pause button
                            Box(
                                modifier =
                                    Modifier
                                        .size(48.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape,
                                        )
                                        .clip(CircleShape)
                                        .clickable { onPlayPauseToggle() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            // Remaining/Total duration label
                            Text(
                                text = FormatUtils.formatDuration(durationMs),
                                style =
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }

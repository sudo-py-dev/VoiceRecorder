package com.voicerecorder.presentation.ui.recorder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicerecorder.R
import com.voicerecorder.domain.model.RecordingState
import com.voicerecorder.presentation.theme.FinalTalkTheme
import com.voicerecorder.presentation.ui.recorder.components.WaveformVisualizer
import com.voicerecorder.presentation.ui.util.FormatUtils

@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val state by viewModel.recordingState.collectAsState()
    val amplitudes by viewModel.amplitudeFlow.collectAsState()
    val errorMsg by viewModel.saveError.collectAsState()
    val successMsg by viewModel.saveSuccess.collectAsState()

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMsg) {
        successMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(FinalTalkTheme.gradients.backgroundGradient)
                .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Status Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                val statusText =
                    when (state) {
                        is RecordingState.Idle -> stringResource(R.string.state_idle)
                        is RecordingState.Recording -> stringResource(R.string.state_recording)
                        is RecordingState.Paused -> stringResource(R.string.state_paused)
                    }
                val statusColor =
                    when (state) {
                        is RecordingState.Recording -> MaterialTheme.colorScheme.primary
                        is RecordingState.Paused -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    }

                Text(
                    text = statusText,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = statusColor,
                )
            }

            // Waveform Display Container
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                WaveformVisualizer(amplitudes = amplitudes)
            }

            // Timer Text and Recording Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp),
            ) {
                val durationMs =
                    when (val s = state) {
                        is RecordingState.Recording -> s.durationMs
                        is RecordingState.Paused -> s.durationMs
                        else -> 0L
                    }

                Text(
                    text = FormatUtils.formatDurationWithDeciseconds(durationMs),
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 52.sp,
                        ),
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(48.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Pause/Resume Option Button
                    AnimatedVisibility(
                        visible = state !is RecordingState.Idle,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(56.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape,
                                    )
                                    .clip(CircleShape)
                                    .clickable {
                                        if (state is RecordingState.Recording) {
                                            viewModel.pauseRecording(context)
                                        } else if (state is RecordingState.Paused) {
                                            viewModel.resumeRecording(context)
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector =
                                    if (state is RecordingState.Recording) {
                                        Icons.Default.Pause
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Animated Core Record Button
                    RecordButton(
                        isRecording = state is RecordingState.Recording,
                        onClick = {
                            if (state is RecordingState.Idle) {
                                viewModel.startRecording(context)
                            } else {
                                viewModel.stopRecording(context)
                            }
                        },
                    )

                    Spacer(modifier = Modifier.width(32.dp))

                    // Stop Option Button
                    AnimatedVisibility(
                        visible = state !is RecordingState.Idle,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(56.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape,
                                    )
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.stopRecording(context)
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animate size and shape based on state
    val buttonSize by animateDpAsState(
        targetValue = if (isRecording) 76.dp else 84.dp,
        animationSpec = tween(300),
        label = "size",
    )

    // Dynamic corner radius morphing circle to square
    val cornerPercent by animateDpAsState(
        targetValue = if (isRecording) 16.dp else 42.dp,
        animationSpec = tween(300),
        label = "shape",
    )

    val color by animateColorAsState(
        targetValue = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "color",
    )

    // Smoothly animate inner icon size and corner shape
    val innerSize by animateDpAsState(
        targetValue = if (isRecording) 24.dp else 28.dp,
        animationSpec = tween(300),
        label = "innerSize",
    )
    val innerCorner by animateDpAsState(
        targetValue = if (isRecording) 4.dp else 14.dp,
        animationSpec = tween(300),
        label = "innerCorner",
    )

    Box(
        modifier =
            Modifier
                .size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Main button
        Box(
            modifier =
                Modifier
                    .size(buttonSize)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(cornerPercent),
                    )
                    .clip(RoundedCornerShape(cornerPercent))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(innerSize)
                        .background(Color.White, RoundedCornerShape(innerCorner)),
            )
        }
    }
}

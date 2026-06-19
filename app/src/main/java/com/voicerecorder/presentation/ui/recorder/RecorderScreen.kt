package com.voicerecorder.presentation.ui.recorder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.voicerecorder.presentation.ui.util.glassmorphic
import com.voicerecorder.presentation.ui.util.neonAura
import com.voicerecorder.presentation.ui.util.weightlessDrift

@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    snackbarHostState: SnackbarHostState,
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
            // Floating Status Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .weightlessDrift(durationMs = 4000, minDrift = -3f, maxDrift = 3f),
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
                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    }

                Box(
                    modifier = Modifier
                        .neonAura(color = statusColor, alpha = 0.12f, radiusFraction = 0.8f)
                        .glassmorphic(RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = statusColor,
                    )
                }
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

                // Floating timer text with glowing neon backdrop
                Box(
                    modifier = Modifier
                        .weightlessDrift(durationMs = 3200, minDrift = -4f, maxDrift = 4f)
                        .neonAura(
                            color = if (state is RecordingState.Recording) MaterialTheme.colorScheme.primary else Color.Transparent,
                            alpha = 0.18f,
                            radiusFraction = 0.9f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = FormatUtils.formatDurationWithDeciseconds(durationMs),
                        style =
                            MaterialTheme.typography.displayLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 56.sp,
                                letterSpacing = (-1).sp
                            ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Gravity Anchor / Orbital Controls Arrangement
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val orbitalDistance = 96.dp
                    
                    val pauseOffsetVal by animateDpAsState(
                        targetValue = if (state !is RecordingState.Idle) -orbitalDistance else 0.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                        label = "pauseOffset"
                    )
                    
                    val stopOffsetVal by animateDpAsState(
                        targetValue = if (state !is RecordingState.Idle) orbitalDistance else 0.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                        label = "stopOffset"
                    )
                    
                    val orbitalAlpha by animateFloatAsState(
                        targetValue = if (state !is RecordingState.Idle) 1f else 0f,
                        animationSpec = tween(400),
                        label = "orbitalAlpha"
                    )

                    // Orbital: Pause / Resume Button
                    Box(
                        modifier = Modifier
                            .offset(x = pauseOffsetVal)
                            .graphicsLayer {
                                alpha = orbitalAlpha
                                scaleX = orbitalAlpha
                                scaleY = orbitalAlpha
                            }
                            .weightlessDrift(durationMs = 2800, minDrift = -3f, maxDrift = 3f)
                            .size(56.dp)
                            .glassmorphic(CircleShape)
                            .clip(CircleShape)
                            .clickable(enabled = state !is RecordingState.Idle) {
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
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    // Orbital: Stop Button
                    Box(
                        modifier = Modifier
                            .offset(x = stopOffsetVal)
                            .graphicsLayer {
                                alpha = orbitalAlpha
                                scaleX = orbitalAlpha
                                scaleY = orbitalAlpha
                            }
                            .weightlessDrift(durationMs = 3200, minDrift = -3f, maxDrift = 3f)
                            .size(56.dp)
                            .glassmorphic(CircleShape)
                            .clip(CircleShape)
                            .clickable(enabled = state !is RecordingState.Idle) {
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

                    // Central Gravity Core: Pulsating Record Button
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

    // Heartbeat pulse animation for the outer glow when recording
    val infiniteTransition = rememberInfiniteTransition(label = "RecordPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Shape and size animations morphing from circle to rounded square
    val cornerPercent by animateDpAsState(
        targetValue = if (isRecording) 14.dp else 34.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "shape"
    )
    
    val buttonSize by animateDpAsState(
        targetValue = if (isRecording) 44.dp else 60.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "size"
    )

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(100.dp)
            .weightlessDrift(durationMs = 4000, minDrift = -2f, maxDrift = 2f),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing neon aura
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .neonAura(color = colorScheme.error, alpha = 0.35f, radiusFraction = 0.95f)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .neonAura(color = colorScheme.primary, alpha = 0.15f, radiusFraction = 0.95f)
            )
        }

        // Solid Outer Ring (mechanical boundary)
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(
                    width = 4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.onBackground.copy(alpha = 0.25f),
                            colorScheme.onBackground.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Morphing Inner Core
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isRecording) {
                                listOf(colorScheme.error, Color(0xFFEF4444))
                            } else {
                                listOf(colorScheme.primary, colorScheme.secondary)
                            }
                        ),
                        shape = RoundedCornerShape(cornerPercent)
                    )
                    .clip(RoundedCornerShape(cornerPercent))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
            )
        }
    }
}

package com.voicerecorder.presentation.ui.util

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voicerecorder.presentation.theme.FinalTalkTheme

/**
 * Applies a smooth weightless floating/drifting motion.
 */
@Composable
fun Modifier.weightlessDrift(
    durationMs: Int = 3500,
    minDrift: Float = -6f,
    maxDrift: Float = 6f
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "WeightlessDrift")
    val driftY by infiniteTransition.animateFloat(
        initialValue = minDrift,
        targetValue = maxDrift,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftY"
    )
    return this.offset(y = driftY.dp)
}

/**
 * Interactive gyro-emulated parallax modifier using drag gestures.
 * Best used on non-scrollable panels.
 */
@Composable
fun Modifier.gyroParallax(
    maxRotationX: Float = 12f,
    maxRotationY: Float = 12f
): Modifier {
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }

    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotationX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotationY"
    )

    return this
        .graphicsLayer {
            this.rotationX = animatedRotationX
            this.rotationY = animatedRotationY
            this.cameraDistance = 16f * density
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    rotationY = (rotationY + dragAmount.x * 0.12f).coerceIn(-maxRotationY, maxRotationY)
                    rotationX = (rotationX - dragAmount.y * 0.12f).coerceIn(-maxRotationX, maxRotationX)
                },
                onDragEnd = {
                    rotationX = 0f
                    rotationY = 0f
                },
                onDragCancel = {
                    rotationX = 0f
                    rotationY = 0f
                }
            )
        }
}

/**
 * Scroll-safe interactive parallax tilt modifier using tap/touch positions.
 */
@Composable
fun Modifier.magneticTilt(
    maxRotationX: Float = 8f,
    maxRotationY: Float = 8f
): Modifier {
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }

    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotationX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotationY"
    )

    return this
        .graphicsLayer {
            this.rotationX = animatedRotationX
            this.rotationY = animatedRotationY
            this.cameraDistance = 12f * density
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.first()
                    val position = change.position
                    val size = this.size

                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    if (change.pressed) {
                        val pctX = (position.x - centerX) / centerX
                        val pctY = (position.y - centerY) / centerY
                        rotationY = (pctX * maxRotationY).coerceIn(-maxRotationY, maxRotationY)
                        rotationX = (-pctY * maxRotationX).coerceIn(-maxRotationX, maxRotationX)
                    } else {
                        rotationX = 0f
                        rotationY = 0f
                    }
                }
            }
        }
}

/**
 * Adds an omnidirectional radial glowing drop shadow backdrop.
 */
fun Modifier.neonAura(
    color: Color,
    alpha: Float = 0.15f,
    radiusFraction: Float = 0.8f
): Modifier {
    if (color == Color.Transparent || color.alpha == 0f) return this
    return this.drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                radius = size.maxDimension * radiusFraction
            )
        )
    }
}

/**
 * Applies premium glassmorphism background and gradient border.
 */
@Composable
fun Modifier.glassmorphic(
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp
): Modifier {
    val gradients = FinalTalkTheme.gradients
    return this
        .background(
            brush = gradients.glassBackground,
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = gradients.glassBorder,
            shape = shape
        )
}

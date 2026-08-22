package com.cr.tunnel.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedHomeBackground(isDarkTheme: Boolean) {
    val transition = rememberInfiniteTransition(label = "homeBackground")

    val sweep = transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    val orb1X = transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb1X"
    )
    val orb1Y = transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(31000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb1Y"
    )
    val orb1A = transition.animateFloat(
        initialValue = 0.5f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb1A"
    )

    val orb2X = transition.animateFloat(
        initialValue = 0.85f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb2X"
    )
    val orb2Y = transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(36000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb2Y"
    )
    val orb2A = transition.animateFloat(
        initialValue = 0.85f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb2A"
    )

    val orb3Pulse = transition.animateFloat(
        initialValue = 0.78f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(8200, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb3Pulse"
    )

    val baseTop = if (isDarkTheme) Color(0xFF060C1E) else Color(0xFFF0F8FE)
    val baseBottom = if (isDarkTheme) Color(0xFF0B1430) else Color(0xFFF8F2FC)
    val sweepCyan = if (isDarkTheme) Color(0x1400E5FF) else Color(0x1000A8C4)
    val sweepPurple = if (isDarkTheme) Color(0x12A855F7) else Color(0x12B06EF0)
    val orbCyan = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF00A8C4)
    val orbPurple = if (isDarkTheme) Color(0xFFA855F7) else Color(0xFFB06EF0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(baseTop, baseBottom)))
            .drawBehind {
                val sweepValue = sweep.value
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(sweepCyan, sweepPurple),
                        start = Offset(-size.width * 0.2f, size.height * sweepValue),
                        end = Offset(size.width * 1.2f, size.height * (sweepValue + 1.1f))
                    )
                )
            }
    ) {
        GlowOrb(
            size = 420.dp,
            color = orbCyan,
            progressX = orb1X,
            progressY = orb1Y,
            alpha = orb1A,
            driftX = 150.dp,
            driftY = 100.dp
        )
        GlowOrb(
            size = 470.dp,
            color = orbPurple,
            progressX = orb2X,
            progressY = orb2Y,
            alpha = orb2A,
            driftX = 120.dp,
            driftY = 140.dp
        )
        GlowOrb(
            size = 230.dp,
            color = orbCyan,
            progressX = null,
            progressY = null,
            alpha = orb3Pulse,
            pulse = orb3Pulse,
            driftX = 0.dp,
            driftY = 0.dp
        )
    }
}

@Composable
private fun GlowOrb(
    size: Dp,
    color: Color,
    progressX: State<Float>?,
    progressY: State<Float>?,
    alpha: State<Float>,
    pulse: State<Float>? = null,
    driftX: Dp,
    driftY: Dp
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val px = driftX.toPx()
                    val py = driftY.toPx()
                    translationX = ((progressX?.value ?: 0.5f) - 0.5f) * px
                    translationY = ((progressY?.value ?: 0.5f) - 0.5f) * py
                    val pulseValue = pulse?.value ?: 1f
                    scaleX = pulseValue
                    scaleY = pulseValue
                    this.alpha = alpha.value
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
    }
}
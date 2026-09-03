package com.cr.tunnel.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin

// Ultra-light Home background: 2 phase drivers only, no per-frame allocation,
// no huge radial-gradient Box layers. Positions derived with trig in draw phase.
@Composable
fun AnimatedHomeBackground(isDarkTheme: Boolean) {
    val transition = rememberInfiniteTransition(label = "homeBackground")

    val phaseA by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing)),
        label = "phaseA"
    )
    val phaseB by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(34000, easing = LinearEasing)),
        label = "phaseB"
    )

    val baseTop = if (isDarkTheme) Color(0xFF060C1E) else Color(0xFFF0F8FE)
    val baseBottom = if (isDarkTheme) Color(0xFF0B1430) else Color(0xFFF8F2FC)
    val orbCyan = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF00A8C4)
    val orbPurple = if (isDarkTheme) Color(0xFFA855F7) else Color(0xFFB06EF0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(baseTop, baseBottom)))
            .drawWithCache {
                val orbR1 = size.minDimension * 0.28f
                val orbR2 = size.minDimension * 0.32f
                onDrawBehind {
                    val w = size.width
                    val h = size.height
                    // Soft translucent circles, positions from trig (cheap, no allocation)
                    val c1x = w * (0.5f + 0.3f * sin(phaseA).toFloat())
                    val c1y = h * (0.32f + 0.12f * sin(phaseB * 0.7f).toFloat())
                    val c2x = w * (0.5f + 0.32f * sin(phaseB + PI.toFloat()).toFloat())
                    val c2y = h * (0.6f + 0.12f * sin(phaseA * 0.8f + 1f).toFloat())
                    drawCircle(
                        color = orbCyan.copy(alpha = 0.10f),
                        radius = orbR1,
                        center = androidx.compose.ui.geometry.Offset(c1x, c1y)
                    )
                    drawCircle(
                        color = orbPurple.copy(alpha = 0.10f),
                        radius = orbR2,
                        center = androidx.compose.ui.geometry.Offset(c2x, c2y)
                    )
                }
            }
    )
}

package com.cr.tunnel.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

val neonCyan = Color(0xFF00F5FF)
val neonPurple = Color(0xFFB44BFF)
val neonPink = Color(0xFFFF2D78)
val neonGreen = Color(0xFF00FFA8)

@Composable
fun NeonAuroraBackground(
    darkTheme: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "neonAurora")

    val phaseA by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "phaseA"
    )
    val phaseB by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing)),
        label = "phaseB"
    )
    val phaseC by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(33000, easing = LinearEasing)),
        label = "phaseC"
    )
    val gridScroll by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "gridScroll"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val particles = rememberParticleOffsets(26)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (darkTheme) {
                        listOf(Color(0xFF000000), Color(0xFF03060F), Color(0xFF000000))
                    } else {
                        listOf(Color(0xFF05070F), Color(0xFF08101F), Color(0xFF04060C))
                    }
                )
            )
            .drawWithCache {
                val w = size.width
                val h = size.height
                val minDim = size.minDimension

                val orbR = minDim * 0.42f

                val gridStep = (h / 16f).coerceAtLeast(1f)
                val gridOffset = (gridScroll * gridStep)

                onDrawBehind {
                    clipRect {
                        // Aurora orbs
                        val c1 = Offset(
                            w * (0.5f + 0.34f * sin(phaseA).toFloat()),
                            h * (0.28f + 0.16f * sin(phaseB * 0.6f + 1f).toFloat())
                        )
                        val c2 = Offset(
                            w * (0.5f + 0.36f * sin(phaseB + PI.toFloat())),
                            h * (0.62f + 0.16f * sin(phaseA * 0.75f).toFloat())
                        )
                        val c3 = Offset(
                            w * (0.5f + 0.30f * sin(phaseC * 0.9f + 2f).toFloat()),
                            h * (0.45f + 0.22f * sin(phaseA * 0.5f + phaseC * 0.3f).toFloat())
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    neonCyan.copy(alpha = 0.16f * breathe),
                                    neonCyan.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = c1,
                                radius = orbR
                            ),
                            radius = orbR,
                            center = c1
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    neonPurple.copy(alpha = 0.15f * breathe),
                                    neonPurple.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = c2,
                                radius = orbR * 1.05f
                            ),
                            radius = orbR * 1.05f,
                            center = c2
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    neonPink.copy(alpha = 0.10f * breathe),
                                    neonPink.copy(alpha = 0.03f),
                                    Color.Transparent
                                ),
                                center = c3,
                                radius = orbR * 0.8f
                            ),
                            radius = orbR * 0.8f,
                            center = c3
                        )

                        // Perspective-ish grid
                        val gridColor = neonCyan.copy(alpha = 0.055f)
                        var y = -gridStep + gridOffset
                        while (y < h + gridStep) {
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                            y += gridStep
                        }
                        val columnStep = (w / 10f).coerceAtLeast(1f)
                        var x = 0f
                        while (x < w + columnStep) {
                            drawLine(
                                color = neonPurple.copy(alpha = 0.045f),
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1f
                            )
                            x += columnStep
                        }

                        // Drifting particles
                        for (i in particles.indices) {
                            val seed = particles[i]
                            val speed = 0.055f + seed.speed
                            val progress = ((gridScroll * speed + seed.phase) % 1f + 1f) % 1f
                            val px = seed.x * w + sin(phaseC + i) * 10f
                            val py = h * (1.05f - progress)
                            val alpha = (0.30f * sin(progress * PI).toFloat()).coerceIn(0f, 0.30f)
                            val radius = (1.1f + seed.speed * 6f).dp.toPx()
                            drawCircle(
                                color = seed.color.copy(alpha = alpha),
                                radius = radius,
                                center = Offset(px, py)
                            )
                        }

                        // Slow moving scan beam
                        val beamY = h * ((gridScroll * 1.7f) % 1f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    neonCyan.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = beamY - h * 0.12f,
                                endY = beamY + h * 0.12f
                            ),
                            topLeft = Offset(0f, beamY - h * 0.12f),
                            size = Size(w, h * 0.24f)
                        )
                    }
                }
            }
    ) {
        content()
    }
}

private class ParticleSeed(
    val x: Float,
    val phase: Float,
    val speed: Float,
    val color: Color
)

@Composable
private fun rememberParticleOffsets(count: Int): List<ParticleSeed> {
    return androidx.compose.runtime.remember(count) {
        val palette = listOf(neonCyan, neonPurple, neonPink, neonGreen)
        List(count) { i ->
            val a = ((i * 2654435761L) % 10000L) / 10000f
            val b = ((i * 40503L + 97L) % 10000L) / 10000f
            val c = ((i * 20183L + 13L) % 10000L) / 10000f
            ParticleSeed(
                x = 0.04f + a * 0.92f,
                phase = b,
                speed = 0.02f + c * 0.05f,
                color = palette[i % palette.size]
            )
        }
    }
}

@Composable
fun NeonHairline(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "neonHairline")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "hairlinePhase"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .drawWithCache {
                val w = size.width
                onDrawBehind {
                    val shift = (phase * 2f - 1f) * w
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                neonCyan.copy(alpha = 0.15f),
                                neonCyan.copy(alpha = 0.85f),
                                neonPurple.copy(alpha = 0.85f),
                                Color.Transparent
                            ),
                            start = Offset(shift - w * 0.75f, 0f),
                            end = Offset(shift + w * 0.75f, 0f)
                        )
                    )
                }
            }
    )
}

fun Modifier.neonRingGlow(
    color: Color,
    radius: androidx.compose.ui.unit.Dp,
    alpha: Float = 1f
): Modifier = this.drawWithCache {
    val center = Offset(size.width / 2f, size.height / 2f)
    val baseRadiusPx = radius.toPx()
    onDrawBehind {
        repeat(3) { index ->
            val extra = (index + 1) * 5f
            val strokeAlpha = (0.16f - index * 0.045f) * alpha
            if (strokeAlpha <= 0f) return@repeat
            drawCircle(
                color = color.copy(alpha = strokeAlpha),
                radius = baseRadiusPx + extra,
                center = center,
                style = Stroke(width = (6f - index).coerceAtLeast(2f))
            )
        }
    }
}

fun Modifier.neonRowGlow(alpha: Float = 1f): Modifier = this.drawWithCache {
    val radius = 22.dp.toPx()
    onDrawBehind {
        val colors = listOf(
            neonCyan.copy(alpha = 0.20f * alpha),
            neonPurple.copy(alpha = 0.16f * alpha)
        )
        repeat(2) { index ->
            val grow = (index + 1) * 5f
            drawRoundRect(
                brush = Brush.linearGradient(colors.map { it.copy(alpha = it.alpha * (1f - index * 0.4f)) }),
                topLeft = Offset(-grow, -grow),
                size = Size(size.width + grow * 2, size.height + grow * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius + grow),
                style = Stroke(width = (5f - index * 1.5f).coerceAtLeast(1.5f))
            )
        }
    }
}

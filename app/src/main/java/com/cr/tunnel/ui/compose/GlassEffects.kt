package com.cr.tunnel.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val glassCyan = Color(0xFF00E5FF)
val glassPurple = Color(0xFFA855F7)
val glassPink = Color(0xFFFF2D78)
val glassGreen = Color(0xFF00D68F)

fun DrawScope.drawGlassHighlights(
    cornerRadius: Dp,
    topAlpha: Float = 0.18f,
    bottomAlpha: Float = 0.05f,
    edgeAlpha: Float = 0.35f
) {
    val radius = cornerRadius.toPx()
    // top inner highlight (light catching the glass edge)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = topAlpha),
                Color.Transparent
            ),
            startY = 0f,
            endY = size.height * 0.45f
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
    )
    // bottom subtle reflection
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = bottomAlpha)
            ),
            startY = size.height * 0.7f,
            endY = size.height
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
    )
    // glass edge (border glow)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = edgeAlpha),
                glassCyan.copy(alpha = edgeAlpha * 0.7f),
                glassPurple.copy(alpha = edgeAlpha * 0.5f),
                Color.White.copy(alpha = edgeAlpha * 0.2f)
            )
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    )
    // tiny specular glints on top-left
    drawCircle(
        color = Color.White.copy(alpha = topAlpha * 0.9f),
        radius = size.minDimension * 0.03f,
        center = Offset(size.width * 0.16f, size.height * 0.14f)
    )
    drawCircle(
        color = Color.White.copy(alpha = topAlpha * 0.5f),
        radius = size.minDimension * 0.014f,
        center = Offset(size.width * 0.24f, size.height * 0.1f)
    )
}

/**
 * Liquid-glass background: deep gradient + aurora blobs + subtle noise-ish overlay.
 * Wrap the whole screen with this.
 */
@Composable
fun GlassBackground(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (darkTheme) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF05070F),
                            Color(0xFF0A0E27),
                            Color(0xFF0F1530)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE9F6FF),
                            Color(0xFFF5FAFC),
                            Color(0xFFEFF3FF)
                        )
                    )
                }
            )
            .drawBehind {
                // aurora blob 1 (cyan)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glassCyan.copy(alpha = if (darkTheme) 0.10f else 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * (0.25f + 0.15f * drift), size.height * 0.2f),
                        radius = size.maxDimension * 0.5f
                    ),
                    radius = size.maxDimension * 0.5f,
                    center = Offset(size.width * (0.25f + 0.15f * drift), size.height * 0.2f)
                )
                // aurora blob 2 (purple)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glassPurple.copy(alpha = if (darkTheme) 0.12f else 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * (0.8f - 0.1f * drift), size.height * 0.55f),
                        radius = size.maxDimension * 0.55f
                    ),
                    radius = size.maxDimension * 0.55f,
                    center = Offset(size.width * (0.8f - 0.1f * drift), size.height * 0.55f)
                )
                // aurora blob 3 (green/pink)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glassGreen.copy(alpha = if (darkTheme) 0.07f else 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * (0.85f - 0.1f * drift)),
                        radius = size.maxDimension * 0.45f
                    ),
                    radius = size.maxDimension * 0.45f,
                    center = Offset(size.width * 0.5f, size.height * (0.85f - 0.1f * drift))
                )
            }
    ) {
        content()
    }
}

/**
 * Liquid-glass card: translucent fill + blur-like highlights + gradient edge.
 */
@Composable
fun GlassCard(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    fillAlpha: Float = if (darkTheme) 0.10f else 0.14f,
    edgeAlpha: Float = 0.35f,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (darkTheme) {
                    Color(0xFFB8C7FF).copy(alpha = fillAlpha)
                } else {
                    Color.White.copy(alpha = fillAlpha)
                }
            )
            .drawBehind {
                drawGlassHighlights(
                    cornerRadius = cornerRadius,
                    topAlpha = if (darkTheme) 0.10f else 0.30f,
                    bottomAlpha = if (darkTheme) 0.04f else 0.10f,
                    edgeAlpha = edgeAlpha
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Glass button with animated gradient border glow.
 */
@Composable
fun GlassButton(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    glow: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "glassBtn")
    val glowPulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (darkTheme) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A2447).copy(alpha = 0.55f),
                            Color(0xFF232E5A).copy(alpha = 0.45f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color(0xFFE9F6FF).copy(alpha = 0.55f)
                        )
                    )
                }
            )
            .drawBehind {
                drawGlassHighlights(
                    cornerRadius = cornerRadius,
                    topAlpha = if (darkTheme) 0.08f else 0.25f,
                    bottomAlpha = 0.03f,
                    edgeAlpha = 0.35f
                )
            }
            .border(
                1.dp,
                if (glow) {
                    Brush.linearGradient(
                        listOf(
                            glassCyan.copy(alpha = 0.9f * glowPulse),
                            glassPurple.copy(alpha = 0.9f * glowPulse)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.08f))
                    )
                },
                RoundedCornerShape(cornerRadius)
            )
            .clickable(onClick = onClick)
    ) {
        content()
    }
}

/**
 * Rotating dashed ring (like a frosted-glass orbit) used as decoration.
 */
@Composable
fun GlassOrbit(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    color: Color = glassCyan,
    durationMillis: Int = 14000
) {
    val transition = rememberInfiniteTransition(label = "orbit")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    Box(
        modifier = modifier.drawBehind {
            rotate(degrees = angle) {
                drawCircle(
                    brush = Brush.linearGradient(
                        listOf(
                            Color.Transparent,
                            color.copy(alpha = if (darkTheme) 0.4f else 0.25f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 14f))
                    )
                )
            }
        }
    )
}
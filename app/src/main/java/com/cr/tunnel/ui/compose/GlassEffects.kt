package com.cr.tunnel.ui.compose

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val glassCyan = Color(0xFF00E5FF)
val glassPurple = Color(0xFFA855F7)
val glassPink = Color(0xFFFF2D78)
val glassGreen = Color(0xFF00D68F)

@Composable
fun GlassBackground(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = if (darkTheme) {
        listOf(Color(0xFF05070F), Color(0xFF0A0E27), Color(0xFF0F1530))
    } else {
        listOf(Color(0xFFE9F6FF), Color(0xFFF5FAFC), Color(0xFFEFF3FF))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = colors))
    ) {
        content()
    }
}

/** Frosted glass tile: rounded translucent fill with a specular top highlight and a thin gradient edge. */
fun Modifier.glassSurface(
    darkTheme: Boolean,
    cornerRadius: Dp = 18.dp,
    fillAlpha: Float = if (darkTheme) 0.12f else 0.18f,
    edgeAlpha: Float = 0.38f
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        if (darkTheme) Color(0xFF22305C).copy(alpha = fillAlpha)
        else Color.White.copy(alpha = fillAlpha)
    )
    .drawWithCache {
        val radius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
        val highlightBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (darkTheme) 0.10f else 0.32f),
                Color.Transparent
            ),
            startY = 0f,
            endY = size.height * 0.5f
        )
        val edgeBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = edgeAlpha),
                glassCyan.copy(alpha = edgeAlpha * 0.6f),
                glassPurple.copy(alpha = edgeAlpha * 0.45f),
                Color.White.copy(alpha = edgeAlpha * 0.15f)
            )
        )
        onDrawBehind {
            drawRoundRect(brush = highlightBrush, cornerRadius = radius)
            drawRoundRect(
                brush = edgeBrush,
                cornerRadius = radius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }
    }

/** Soft frosted fill without an edge, used behind form fields where the outline is drawn by the field itself. */
fun Modifier.glassFill(
    darkTheme: Boolean,
    cornerRadius: Dp = 14.dp,
    fillAlpha: Float = if (darkTheme) 0.10f else 0.16f
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        if (darkTheme) Color(0xFFB8C7FF).copy(alpha = fillAlpha)
        else Color.White.copy(alpha = fillAlpha)
    )
    .drawWithCache {
        val radius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
        val highlightBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (darkTheme) 0.08f else 0.30f),
                Color.Transparent
            ),
            startY = 0f,
            endY = size.height * 0.45f
        )
        onDrawBehind {
            drawRoundRect(brush = highlightBrush, cornerRadius = radius)
        }
    }

@Composable
fun glassDialogColor(): Color {
    val dark = LocalDarkTheme.current
    return if (dark) Color(0xFF101A3C).copy(alpha = 0.95f)
    else Color(0xFFF2F9FF).copy(alpha = 0.97f)
}

@Composable
fun GlassCard(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    fillAlpha: Float = if (darkTheme) 0.12f else 0.18f,
    edgeAlpha: Float = 0.38f,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.glassSurface(
            darkTheme = darkTheme,
            cornerRadius = cornerRadius,
            fillAlpha = fillAlpha,
            edgeAlpha = edgeAlpha
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun GlassButton(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    glow: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
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
            .drawWithCache {
                val topBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (darkTheme) 0.08f else 0.25f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.45f
                )
                onDrawBehind {
                    drawRoundRect(
                        brush = topBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                }
            }
            .border(
                1.dp,
                if (glow) {
                    Brush.linearGradient(
                        listOf(
                            glassCyan.copy(alpha = 0.9f),
                            glassPurple.copy(alpha = 0.9f)
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

@Composable
fun GlassOrbit(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    color: Color = glassCyan
) {
    Box(
        modifier = modifier.drawWithCache {
            val orbitBrush = Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    color.copy(alpha = if (darkTheme) 0.4f else 0.25f),
                    Color.Transparent
                )
            )
            onDrawBehind {
                drawCircle(
                    brush = orbitBrush,
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
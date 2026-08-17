package com.cr.tunnel.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cr.tunnel.R
import com.cr.tunnel.ui.compose.colorPing
import com.cr.tunnel.ui.compose.drawGlassHighlights

private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFFA855F7)

@Composable
fun ConnectionSection(
    displayText: String,
    isRunning: Boolean,
    isAutoOptimizing: Boolean,
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    onTest: () -> Unit,
    onAutoOptimize: () -> Unit,
    onCancelAutoOptimize: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConnectionCircle(
            isRunning = isRunning,
            isDarkTheme = isDarkTheme,
            onClick = onToggle
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = displayText,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTest),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isRunning) colorPing else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        val optimizeGlow by rememberInfiniteTransition(label = "optimizeGlow").animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "optimizeGlow"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDarkTheme) Color(0x2200E5FF) else Color(0x0F00A8C4)
                )
                .border(
                    1.dp,
                    if (isAutoOptimizing) Brush.linearGradient(
                        listOf(
                            NeonCyan.copy(alpha = optimizeGlow),
                            NeonPurple.copy(alpha = optimizeGlow)
                        )
                    )
                    else Brush.linearGradient(listOf(Color(0x3300E5FF), Color(0x33A855F7))),
                    RoundedCornerShape(18.dp)
                )
                .clickable(onClick = {
                    if (isAutoOptimizing) onCancelAutoOptimize() else onAutoOptimize()
                })
                .clip(RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = if (isAutoOptimizing) painterResource(R.drawable.ic_flash_off_24dp)
                else painterResource(R.drawable.ic_flash_on_24dp),
                contentDescription = null,
                tint = if (isAutoOptimizing) NeonPurple else NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (isAutoOptimizing) R.string.menu_auto_optimize_cancel
                    else R.string.menu_auto_optimize
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (isAutoOptimizing) NeonPurple else NeonCyan,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ConnectionCircle(
    isRunning: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circle")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterRotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val ringProgress by animateFloatAsState(if (isRunning) 1f else 0f, label = "ring")

    val ringColors = if (isRunning) {
        listOf(colorPing, NeonCyan, colorPing)
    } else {
        listOf(NeonCyan, NeonPurple, NeonCyan)
    }
    val glowColor = if (isRunning) colorPing else NeonCyan

    Box(
        modifier = Modifier
            .size(210.dp)
            .drawBehind {
                val stroke = 10.dp.toPx()
                val arcInset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val arcTopLeft = Offset(arcInset, arcInset)
                rotate(degrees = rotation, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = ringColors,
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                rotate(degrees = counterRotation, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color.Transparent, glowColor.copy(alpha = 0.35f), Color.Transparent),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                val pulseSize = size.width + (36.dp.toPx() * pulse)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = if (isRunning) 0.16f * pulse else 0.05f * pulse),
                            Color.Transparent
                        ),
                        center = center,
                        radius = pulseSize
                    ),
                    radius = pulseSize,
                    center = center
                )
            }
            .scale(breathe)
            .padding(26.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isRunning) 0.22f else 0.08f),
                        Color.Transparent
                    ),
                    radius = 800f
                )
            )
            .clip(CircleShape)
            .background(
                if (isDarkTheme) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF2A3A6E).copy(alpha = 0.45f),
                            Color(0xFF101830).copy(alpha = 0.85f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color(0xFFDFF2FF).copy(alpha = 0.75f)
                        )
                    )
                }
            )
            .drawBehind {
                drawGlassHighlights(
                    cornerRadius = 105.dp,
                    topAlpha = if (isDarkTheme) 0.08f else 0.30f,
                    bottomAlpha = if (isDarkTheme) 0.03f else 0.08f,
                    edgeAlpha = if (isRunning) 0.55f else 0.30f
                )
            }
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(glowColor.copy(alpha = if (isRunning) 0.8f else 0.4f), NeonPurple.copy(alpha = 0.3f))
                ),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = if (isRunning) painterResource(R.drawable.ic_stop_24dp)
                else painterResource(R.drawable.ic_play_24dp),
                contentDescription = stringResource(
                    if (isRunning) R.string.acc_stop else R.string.acc_start
                ),
                tint = if (isRunning) colorPing else NeonCyan,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (isRunning) R.string.acc_stop else R.string.acc_start
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (isRunning) colorPing else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
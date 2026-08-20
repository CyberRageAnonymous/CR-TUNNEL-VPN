package com.cr.tunnel.ui.main

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cr.tunnel.R
import com.cr.tunnel.ui.compose.colorPing
import com.cr.tunnel.ui.compose.glassCyan
import kotlinx.coroutines.delay

private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFFA855F7)

@Composable
fun ConnectionSection(
    displayText: String,
    isRunning: Boolean,
    isAutoOptimizing: Boolean,
    isDarkTheme: Boolean,
    connectedAtMs: Long?,
    uplinkSpeed: String,
    downlinkSpeed: String,
    totalUplink: String,
    totalDownlink: String,
    onToggle: () -> Unit,
    onTest: () -> Unit,
    onAutoOptimize: () -> Unit,
    onCancelAutoOptimize: () -> Unit
) {
    var elapsedSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            elapsedSeconds = 0L
            while (true) {
                delay(1000)
                if (connectedAtMs != null) {
                    elapsedSeconds = (System.currentTimeMillis() - connectedAtMs) / 1000
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isRunning) {
            ProtectedBanner(
                isDarkTheme = isDarkTheme,
                isAutoOptimizing = isAutoOptimizing,
                onAutoOptimize = onAutoOptimize,
                onCancelAutoOptimize = onCancelAutoOptimize
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        ConnectionCircle(
            isRunning = isRunning,
            isDarkTheme = isDarkTheme,
            elapsedSeconds = elapsedSeconds,
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

        if (isRunning) {
            ConnectionStatsBar(
                isDarkTheme = isDarkTheme,
                uplinkSpeed = uplinkSpeed,
                downlinkSpeed = downlinkSpeed,
                totalUplink = totalUplink,
                totalDownlink = totalDownlink
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

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
                            NeonCyan,
                            NeonPurple
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
private fun ProtectedBanner(
    isDarkTheme: Boolean,
    isAutoOptimizing: Boolean,
    onAutoOptimize: () -> Unit,
    onCancelAutoOptimize: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isDarkTheme) Color(0x2200E5FF) else Color(0x0F00A8C4)
            )
            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(18.dp))
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0x3300E5FF), Color(0x33A855F7))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_lock_24dp),
                    contentDescription = null,
                    tint = colorPing,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.protected_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colorPing,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.protected_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkTheme) Color(0x33A855F7) else Color(0x1FA855F7))
                .clickable(onClick = {
                    if (isAutoOptimizing) onCancelAutoOptimize() else onAutoOptimize()
                })
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAutoOptimizing)
                    stringResource(R.string.menu_auto_optimize_cancel)
                else stringResource(R.string.auto_connect_label),
                style = MaterialTheme.typography.labelMedium,
                color = NeonPurple,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ConnectionStatsBar(
    isDarkTheme: Boolean,
    uplinkSpeed: String,
    downlinkSpeed: String,
    totalUplink: String,
    totalDownlink: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isDarkTheme) Color(0x2200E5FF) else Color(0x0F00A8C4)
            )
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.ic_flash_on_24dp),
            contentDescription = null,
            tint = colorPing,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.connection_quality_excellent),
                style = MaterialTheme.typography.titleSmall,
                color = colorPing,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "\u2193 $downlinkSpeed   \u2191 $uplinkSpeed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ConnectionCircle(
    isRunning: Boolean,
    isDarkTheme: Boolean,
    elapsedSeconds: Long,
    onClick: () -> Unit
) {
    val ringColors = if (isRunning) {
        listOf(colorPing, NeonCyan, colorPing)
    } else {
        listOf(NeonCyan, NeonPurple, NeonCyan)
    }
    val glowColor = if (isRunning) colorPing else NeonCyan

    Box(
        modifier = Modifier
            .size(170.dp)
            .drawWithCache {
                val stroke = 8.dp.toPx()
                val arcInset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val arcTopLeft = Offset(arcInset, arcInset)
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val ringBrush = Brush.sweepGradient(
                    colors = ringColors,
                    center = centerOffset
                )
                val glowBrush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, glowColor.copy(alpha = 0.35f), Color.Transparent),
                    center = centerOffset
                )
                onDrawBehind {
                    drawArc(
                        brush = ringBrush,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = glowBrush,
                        startAngle = 0f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            .padding(22.dp)
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
            .drawWithCache {
                val highlightBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDarkTheme) 0.08f else 0.30f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.45f
                )
                val edgeBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isRunning) 0.55f else 0.30f),
                        glassCyan.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.15f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(
                        brush = highlightBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(85.dp.toPx(), 85.dp.toPx())
                    )
                    drawRoundRect(
                        brush = edgeBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(85.dp.toPx(), 85.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }
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
        if (isRunning) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.connected_status),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorPing,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatElapsed(elapsedSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_play_24dp),
                contentDescription = stringResource(R.string.acc_start),
                tint = NeonCyan,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}
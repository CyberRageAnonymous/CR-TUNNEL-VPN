package com.cr.tunnel.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cr.tunnel.R
import com.cr.tunnel.handler.LatencyHistoryManager
import com.cr.tunnel.ui.compose.AppTopBar
import com.cr.tunnel.ui.compose.glassDialogColor
import kotlin.math.max

@Composable
fun LatencyGraphScreen(
    serverGuid: String,
    serverName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val latencyHistory = LatencyHistoryManager.getHistory(serverGuid)
    
    var selectedPeriod by remember { mutableStateOf(Period.Last24Hours) }

    val filteredPoints = remember(latencyHistory, selectedPeriod) {
        latencyHistory.filter { 
            val cutoff = System.currentTimeMillis() - selectedPeriod.durationMs
            it.timestamp >= cutoff
        }.sortedBy { it.timestamp }
    }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.latency_graph_title, serverName),
                onBackClick = onBackClick,
                isLoading = false
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Period selector using simple buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Period.values().forEach { period ->
                    TextButton(
                        onClick = { selectedPeriod = period },
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .padding(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedPeriod == period) 
                                androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer 
                            else 
                                androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (selectedPeriod == period) 
                                androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = period.label,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.padding(top = 16.dp))
            
            // Graph card
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (filteredPoints.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(R.drawable.ic_stats_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.padding(top = 16.dp))
                            Text(
                                text = stringResource(R.string.latency_graph_no_data),
                                fontSize = 16.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LatencyGraphCanvas(points = filteredPoints)
                    }
                }
            }
        }
    }
}

@Composable
fun LatencyGraphCanvas(
    points: List<LatencyHistoryManager.LatencyPoint>,
    modifier: Modifier = Modifier
) {
    val minLatency = remember(points) { points.minOfOrNull { it.latencyMs } ?: 0 }
    val maxLatency = remember(points) { points.maxOfOrNull { it.latencyMs } ?: 100 }
    val latencyRange = maxLatency - minLatency
    val effectiveRange = if (latencyRange > 0) latencyRange else 100
    val minDisplay = max(0, minLatency - effectiveRange / 4)
    val maxDisplay = maxLatency + effectiveRange / 4
    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val gridColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
    val areaColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val padding = 40f
        val graphWidth = width - 2 * padding
        val graphHeight = height - 2 * padding

        // Draw grid lines

        
        // Horizontal grid lines (latency values)
        for (i in 0..4) {
            val y = padding + i * graphHeight / 4
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 0.5f
            )
        }

        val firstPoint = points.first()
        val lastPoint = points.last()
        val timeRange = maxOf(lastPoint.timestamp - firstPoint.timestamp, 1L)
        
        val path = Path()
        
        points.forEachIndexed { index, point ->
            val x = padding + (point.timestamp - firstPoint.timestamp) * graphWidth / timeRange
            val normalizedLatency = (point.latencyMs - minDisplay) / (maxDisplay - minDisplay)
            val y = padding + graphHeight - normalizedLatency * graphHeight
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw area under curve
        val areaPath = Path()
        areaPath.addPath(path)
        areaPath.lineTo(padding + graphWidth, height - padding)
        areaPath.lineTo(padding, height - padding)
        areaPath.close()
        
        drawPath(
            path = areaPath,
            color = areaColor,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Draw line
        drawPath(
            path = path, 
            color = primaryColor, 
            style = Stroke(width = 2.5f)
        )

        // Draw points
        points.forEachIndexed { index, point ->
            val x = padding + (point.timestamp - firstPoint.timestamp) * graphWidth / timeRange
            val normalizedLatency = (point.latencyMs - minDisplay) / (maxDisplay - minDisplay)
            val y = padding + graphHeight - normalizedLatency * graphHeight
            
            drawCircle(
                color = primaryColor,
                center = Offset(x, y),
                radius = 4f
            )
        }
    }
}

enum class Period(val label: String, val durationMs: Long) {
    LastHour("1H", 60 * 60 * 1000L),
    Last24Hours("24H", 24 * 60 * 60 * 1000L),
    Last7Days("7D", 7 * 24 * 60 * 60 * 1000L)
}

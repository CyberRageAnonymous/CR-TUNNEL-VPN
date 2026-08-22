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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cr.tunnel.R
import com.cr.tunnel.ui.compose.LocalDarkTheme

enum class MainTab(@androidx.annotation.DrawableRes val iconRes: Int, @androidx.annotation.StringRes val labelRes: Int) {
    Home(R.drawable.ic_home_24dp, R.string.nav_home),
    Configs(R.drawable.ic_configs_24dp, R.string.nav_configs),
    Stats(R.drawable.ic_stats_24dp, R.string.nav_stats),
    Settings(R.drawable.ic_settings_24dp, R.string.nav_settings)
}

private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFFA855F7)

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    val isDarkTheme = LocalDarkTheme.current
    NavigationBar(
        containerColor = if (isDarkTheme) Color(0xF00F1530) else Color(0xF5FFFFFF),
        tonalElevation = 0.dp
    ) {
        MainTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = stringResource(tab.labelRes),
                        tint = if (selected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    indicatorColor = if (isDarkTheme) Color(0x2200E5FF) else Color(0x1F00A8C4),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun StatsPage(
    isRunning: Boolean,
    uplinkSpeed: String,
    downlinkSpeed: String,
    totalUplink: String,
    totalDownlink: String,
    connectedAtMs: Long?,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current
    var now by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(System.currentTimeMillis()) }

    androidx.compose.runtime.LaunchedEffect(isRunning) {
        while (isRunning) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isDarkTheme) Color(0x2200E5FF) else Color(0x0F00A8C4)
                )
                .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (isRunning) Color(0xFF00E5FF) else Color(0xFFA855F7),
                        RoundedCornerShape(6.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isRunning) stringResource(R.string.stats_connected)
                    else stringResource(R.string.stats_disconnected),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isRunning) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        StatCard(
            title = stringResource(R.string.stats_speed),
            items = listOf(
                stringResource(R.string.stats_downlink) to downlinkSpeed,
                stringResource(R.string.stats_uplink) to uplinkSpeed
            ),
            isDarkTheme = isDarkTheme
        )

        StatCard(
            title = stringResource(R.string.stats_session_title),
            items = listOf(
                stringResource(R.string.stats_downlink) to totalDownlink,
                stringResource(R.string.stats_uplink) to totalUplink
            ),
            isDarkTheme = isDarkTheme
        )

        StatCard(
            title = stringResource(R.string.stats_uptime),
            items = listOf(
                if (isRunning && connectedAtMs != null) {
                    formatElapsedForStats(connectedAtMs, now)
                } else {
                    "—"
                } to ""
            ),
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    items: List<Pair<String, String>>,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isDarkTheme) Color(0x2200E5FF) else Color(0x0F00A8C4)
            )
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Bold
        )
        items.forEach { (label, value) ->
            if (label.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsPage(
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current
    val items = listOf(
        MainDestination.Subscriptions,
        MainDestination.PerAppProxy,
        MainDestination.Routing,
        MainDestination.UserAssets,
        MainDestination.Settings,
        MainDestination.Community,
        MainDestination.Promotion,
        MainDestination.Logcat,
        MainDestination.CheckUpdate,
        MainDestination.BackupRestore,
        MainDestination.About
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDarkTheme) Color(0x1AFFFFFF) else Color(0x0F00A8C4)
                    )
                    .clickable { onNavigate(item) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    tint = if (item == MainDestination.Settings) NeonPurple else NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(item.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatElapsedForStats(connectedAtMs: Long, now: Long = System.currentTimeMillis()): String {
    val totalSeconds = (now - connectedAtMs).coerceAtLeast(0L) / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}
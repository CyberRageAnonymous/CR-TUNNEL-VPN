package com.cr.tunnel.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cr.tunnel.dto.GroupMapItem
import com.cr.tunnel.dto.entities.ServersCache
import com.cr.tunnel.ui.compose.LocalNeonTheme
import com.cr.tunnel.ui.compose.colorFabActive
import com.cr.tunnel.ui.compose.glassDialogColor
import com.cr.tunnel.ui.compose.neonCyan
import com.cr.tunnel.ui.compose.neonPurple
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun GroupTabBar(
    groups: List<GroupMapItem>,
    selectedTabIndex: Int,
    mainViewModel: MainViewModel,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isNeon = LocalNeonTheme.current
    val transition = rememberInfiniteTransition(label = "neonTab")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing)),
        label = "tabPhase"
    )
    val shift = (sin(phase * 2 * PI).toFloat() + 1f) / 2f
    val activeColor = if (isNeon) lerp(neonCyan, neonPurple, shift) else colorFabActive

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex.coerceIn(0, groups.lastIndex),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        containerColor = glassDialogColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 16.dp,
        minTabWidth = 56.dp,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier
                    .tabIndicatorOffset(
                        selectedTabIndex = selectedTabIndex.coerceIn(0, groups.lastIndex),
                        matchContentSize = true
                    )
                    .clip(RoundedCornerShape(10.dp)),
                width = Dp.Unspecified,
                color = activeColor
            )
        },
        divider = {}
    ) {
        groups.forEachIndexed { index, group ->
            GroupTabItem(
                group = group,
                selected = index == selectedTabIndex,
                activeColor = activeColor,
                serverFlowProvider = { mainViewModel.serversForGroup(group.id) },
                onClick = { onTabClick(index) }
            )
        }
    }
}

@Composable
private fun GroupTabItem(
    group: GroupMapItem,
    selected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    serverFlowProvider: () -> StateFlow<List<ServersCache>>,
    onClick: () -> Unit
) {
    val serverFlow = remember(group.id) { serverFlowProvider() }
    val servers by serverFlow.collectAsStateWithLifecycle()
    val textColor by animateColorAsState(
        targetValue = if (selected) activeColor else MaterialTheme.colorScheme.onSurface,
        label = "tabTextColor"
    )
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            val text = if (group.id.isEmpty()) {
                group.remarks
            } else {
                "${group.remarks} (${servers.size})"
            }
            Text(
                text = text,
                color = textColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

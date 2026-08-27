package com.cr.tunnel.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cr.tunnel.R
import com.cr.tunnel.dto.entities.ProfileItem
import com.cr.tunnel.ui.compose.LocalDarkTheme
import com.cr.tunnel.ui.compose.QRCodeDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.cr.tunnel.util.Utils

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val displayText = uiState.statusText
    val selectedGuid = uiState.selectedGuid
    val doubleColumnDisplay = uiState.doubleColumnDisplay
    val confirmRemove = uiState.confirmRemove
    val shareQRCodeBitmap = uiState.shareQRCodeBitmap

    val isDarkTheme = LocalDarkTheme.current
    val scope = rememberCoroutineScope()
    val updatePrompt by mainViewModel.updatePrompt.collectAsStateWithLifecycle()

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(MainTab.Home) }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf<String?>(null) }

    var shareTarget by remember { mutableStateOf<Triple<String, ProfileItem, Boolean>?>(null) }
    val removeServer: (String) -> Unit = { guid ->
        if (confirmRemove) showRemoveConfirm = guid else onAction(MainAction.RemoveServer(guid))
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { groups.size.coerceAtLeast(1) }
    )

    val lazyListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val lazyGridStates = remember { mutableStateMapOf<String, LazyGridState>() }

    var locateInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(groups) {
        val validGroupIds = groups.map { it.id }.toSet()
        lazyListStates.keys.retainAll(validGroupIds)
        lazyGridStates.keys.retainAll(validGroupIds)
    }

    val latestDoubleColumnDisplay by rememberUpdatedState(doubleColumnDisplay)

    LaunchedEffect(groups, uiState.selectedGroupId) {
        if (groups.isEmpty()) return@LaunchedEffect
        val selectedIndex = groups.indexOfFirst { it.id == uiState.selectedGroupId }
            .takeIf { it >= 0 } ?: 0
        if (!pagerState.isScrollInProgress && pagerState.settledPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    val latestGroups by rememberUpdatedState(groups)
    val latestLocateInProgress by rememberUpdatedState(locateInProgress)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val currentGroups = latestGroups
                if (!latestLocateInProgress && page in currentGroups.indices) {
                    onAction(MainAction.SelectGroup(currentGroups[page].id))
                }
            }
    }

    LaunchedEffect(uiState.locateTarget) {
        val target = uiState.locateTarget ?: return@LaunchedEffect
        if (target.groupIndex !in 0 until pagerState.pageCount) {
            mainViewModel.onAction(MainAction.LocateHandled(target))
            return@LaunchedEffect
        }

        locateInProgress = true
        try {
            if (pagerState.settledPage != target.groupIndex) {
                pagerState.navigateToPageOptimized(
                    targetPage = target.groupIndex,
                    animateAdjacentPage = false
                )
            }
            onAction(MainAction.SelectGroup(target.groupId))

            repeat(10) {
                val ready = if (latestDoubleColumnDisplay) {
                    lazyGridStates[target.groupId] != null
                } else {
                    lazyListStates[target.groupId] != null
                }
                if (ready) return@repeat
                delay(16L)
            }

            if (latestDoubleColumnDisplay) {
                lazyGridStates[target.groupId]?.let { gridState ->
                    gridState.scrollToItem(
                        index = target.itemPosition,
                        scrollOffset = -gridState.layoutInfo.viewportSize.height / 3
                    )
                }
            } else {
                lazyListStates[target.groupId]?.let { listState ->
                    listState.scrollToItem(
                        index = target.itemPosition,
                        scrollOffset = -listState.layoutInfo.viewportSize.height / 3
                    )
                }
            }
        } finally {
            delay(32L)
            locateInProgress = false
            mainViewModel.onAction(MainAction.LocateHandled(target))
        }
    }

    MainDialogs(
        showDelAllConfirm = showDelAllConfirm,
        onDismissDelAll = { showDelAllConfirm = false },
        onConfirmDelAll = { showDelAllConfirm = false; onAction(MainAction.RemoveAllServers) },
        showDelDuplicateConfirm = showDelDuplicateConfirm,
        onDismissDelDuplicate = { showDelDuplicateConfirm = false },
        onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onAction(MainAction.RemoveDuplicateServers) },
        showDelInvalidConfirm = showDelInvalidConfirm,
        onDismissDelInvalid = { showDelInvalidConfirm = false },
        onConfirmDelInvalid = { showDelInvalidConfirm = false; onAction(MainAction.RemoveInvalidServers) },
        showRemoveConfirm = showRemoveConfirm,
        onDismissRemove = { showRemoveConfirm = null },
        onConfirmRemove = { guid -> showRemoveConfirm = null; onAction(MainAction.RemoveServer(guid)) }
    )

    if (shareTarget != null) {
        val (guid, profile, more) = shareTarget!!
        ShareMethodDialog(
            guid = guid,
            profile = profile,
            more = more,
            onDismiss = { shareTarget = null },
            onAction = onAction,
            onRemove = removeServer,
        )
    }
    if (shareQRCodeBitmap != null) {
        QRCodeDialog(bitmap = shareQRCodeBitmap, onDismiss = { onAction(MainAction.DismissQRCodeDialog) })
    }

    updatePrompt?.let { result ->
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { mainViewModel.dismissUpdatePrompt() },
            title = { Text(stringResource(R.string.update_new_version_found, result.latestVersion ?: "")) },
            text = { Text(result.releaseNotes.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    mainViewModel.dismissUpdatePrompt()
                    result.downloadUrl?.let { Utils.openUri(context, it) }
                }) {
                    Text(stringResource(R.string.update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { mainViewModel.dismissUpdatePrompt() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            MainTopBar(
                selectedTab = selectedTab,
                isLoading = isLoading,
                showSearch = showSearch,
                searchQuery = searchQuery,
                onSearchQueryChange = { query: String ->
                    searchQuery = query
                    onAction(MainAction.Search(query))
                },
                onSearchClose = {
                    searchQuery = ""
                    onAction(MainAction.Search(""))
                    showSearch = false
                },
                onSearchToggle = { show: Boolean -> showSearch = show },
                onAction = onAction,
                onMoreMenuAction = { action ->
                    when (action) {
                        MainMoreMenuAction.AutoOptimize -> onAction(MainAction.AutoOptimize)
                        MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
                        MainMoreMenuAction.DeleteAll -> showDelAllConfirm = true
                        MainMoreMenuAction.DeleteDuplicate -> showDelDuplicateConfirm = true
                        MainMoreMenuAction.DeleteInvalid -> showDelInvalidConfirm = true
                        MainMoreMenuAction.ExportAll -> onAction(MainAction.ExportAll)
                        MainMoreMenuAction.LocateSelected -> onAction(MainAction.LocateSelectedServer)
                        MainMoreMenuAction.SortByTestResults -> onAction(MainAction.SortByTestResults)
                        MainMoreMenuAction.TestAll -> onAction(MainAction.TestAllServers)
                        MainMoreMenuAction.TestAllRealPing -> onAction(MainAction.TestRealAllServers)
                        MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
                    }
                }
            )
        },
        bottomBar = {
            MainBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == selectedTab) return@MainBottomBar
                    selectedTab = tab
                    showSearch = false
                }
            )
        },
        floatingActionButton = {},
    ) { innerPadding ->
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                // Mirror the slide direction in RTL locales so pages always
                // move toward the reading side of the user.
                val enterFromEnd = if (isRtl) !forward else forward
                if (enterFromEnd) {
                    (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 4 } +
                        fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 4 } +
                            fadeOut(tween(280)))
                } else {
                    (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 4 } +
                        fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 4 } +
                            fadeOut(tween(280)))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                MainTab.Home -> HomeTab(
                    uiState = uiState,
                    displayText = displayText,
                    isDarkTheme = isDarkTheme,
                    onAction = onAction
                )

                MainTab.Configs -> ConfigsTab(
                    mainViewModel = mainViewModel,
                    groups = groups,
                    selectedGuid = selectedGuid,
                    doubleColumnDisplay = doubleColumnDisplay,
                    confirmRemove = confirmRemove,
                    searchQuery = searchQuery,
                    pagerState = pagerState,
                    lazyListStates = lazyListStates,
                    lazyGridStates = lazyGridStates,
                    scope = scope,
                    onAction = onAction,
                    shareTarget = { guid, profile, more -> shareTarget = Triple(guid, profile, more) },
                    removeServer = removeServer
                )

                MainTab.Stats -> StatsPage(
                    isRunning = isRunning,
                    uplinkSpeed = uiState.uplinkSpeed,
                    downlinkSpeed = uiState.downlinkSpeed,
                    totalUplink = uiState.totalUplink,
                    totalDownlink = uiState.totalDownlink,
                    connectedAtMs = uiState.connectedAtMs,
                    statusText = displayText
                )

                MainTab.Settings -> SettingsPage(onNavigate = onNavigate)
            }
        }
    }
}

@Composable
private fun HomeTab(
    uiState: MainUiState,
    displayText: String,
    isDarkTheme: Boolean,
    onAction: (MainAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AnimatedHomeBackground(isDarkTheme = isDarkTheme)
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = uiState.isAutoOptimizing,
                enter = expandVertically(
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(280)),
                exit = shrinkVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(220))
            ) {
                OptimizeBanner(onCancel = { onAction(MainAction.CancelAutoOptimize) })
            }
            ConnectionSection(
                displayText = displayText,
                isRunning = uiState.isRunning,
                isConnecting = uiState.isConnecting,
                isAutoOptimizing = uiState.isAutoOptimizing,
            isDarkTheme = isDarkTheme,
            connectedAtMs = uiState.connectedAtMs,
            uplinkSpeed = uiState.uplinkSpeed,
            downlinkSpeed = uiState.downlinkSpeed,
            totalUplink = uiState.totalUplink,
            totalDownlink = uiState.totalDownlink,
            onToggle = { onAction(MainAction.ToggleService) },
            onTest = { onAction(MainAction.TestCurrentServer) },
            onAutoOptimize = { onAction(MainAction.AutoOptimize) },
            onCancelAutoOptimize = { onAction(MainAction.CancelAutoOptimize) }
        )
        }
    }
}

@Composable
private fun ConfigsTab(
    mainViewModel: MainViewModel,
    groups: List<com.cr.tunnel.dto.GroupMapItem>,
    selectedGuid: String?,
    doubleColumnDisplay: Boolean,
    confirmRemove: Boolean,
    searchQuery: String,
    pagerState: androidx.compose.foundation.pager.PagerState,
    lazyListStates: MutableMap<String, LazyListState>,
    lazyGridStates: MutableMap<String, LazyGridState>,
    scope: CoroutineScope,
    onAction: (MainAction) -> Unit,
    shareTarget: (String, ProfileItem, Boolean) -> Unit,
    removeServer: (String) -> Unit
) {
    if (groups.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (groups.size > 1) {
            GroupTabBar(
                groups = groups,
                selectedTabIndex = pagerState.currentPage.coerceIn(0, groups.lastIndex),
                mainViewModel = mainViewModel,
                onTabClick = { targetIndex ->
                    scope.launch {
                        pagerState.navigateToPageOptimized(
                            targetPage = targetIndex,
                            animateAdjacentPage = true
                        )
                    }
                }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            beyondViewportPageCount = 1,
            key = { page -> groups.getOrNull(page)?.id ?: "group-page-$page" }
        ) { page ->
            val group = groups.getOrNull(page) ?: return@HorizontalPager

            GroupPagerPage(
                groupId = group.id,
                mainViewModel = mainViewModel,
                selectedGuid = selectedGuid,
                doubleColumnDisplay = doubleColumnDisplay,
                confirmRemove = confirmRemove,
                searchQuery = searchQuery,
                lazyListStates = lazyListStates,
                lazyGridStates = lazyGridStates,
                onSelectServer = { guid -> onAction(MainAction.SelectServer(guid)) },
                onEditServer = { guid, profile -> onAction(MainAction.EditServer(guid, profile)) },
                onShareServer = { guid, profile -> shareTarget(guid, profile, false) },
                onMoreServer = { guid, profile -> shareTarget(guid, profile, true) },
                onRemoveServer = removeServer,
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = 8.dp
                )
            )
        }
    }
}

@Composable
fun OptimizeBanner(onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x3323E5FF),
                        Color(0x33A855F7)
                    )
                )
            )
            .border(1.dp, Color(0x5500E5FF), RoundedCornerShape(50))
            .padding(start = 14.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color(0xFF00E5FF), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.menu_auto_optimize),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            painter = painterResource(R.drawable.ic_close_24dp),
            contentDescription = stringResource(R.string.menu_auto_optimize_cancel),
            tint = Color(0xFFA8B8D0),
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onCancel)
                .padding(5.dp)
        )
    }
}

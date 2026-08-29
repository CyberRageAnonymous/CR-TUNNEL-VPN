package com.cr.tunnel.ui.perappproxy

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cr.tunnel.R
import com.cr.tunnel.dto.AppInfo
import com.cr.tunnel.extension.toastInfo
import com.cr.tunnel.extension.toastSuccess
import com.cr.tunnel.ui.base.BaseComponentActivity
import com.cr.tunnel.ui.compose.AppDivider
import com.cr.tunnel.ui.compose.AppDropdownMenuItems
import com.cr.tunnel.ui.compose.AppTopBar
import com.cr.tunnel.ui.compose.ItemDivider
import com.cr.tunnel.ui.compose.colorFabActive
import com.cr.tunnel.ui.compose.verticalScrollbar
import com.cr.tunnel.util.AppIconFetcher
import com.cr.tunnel.util.Utils

private enum class PerAppMenuAction(@StringRes val labelRes: Int) {
    SelectAll(R.string.menu_item_select_all),
    InvertSelection(R.string.menu_item_invert_selection),
    SelectProxyApps(R.string.menu_item_select_proxy_app),
    ImportSelection(R.string.menu_item_import_proxy_app),
    ExportSelection(R.string.menu_item_export_proxy_app)
}

class PerAppProxyActivity : BaseComponentActivity() {

    private val viewModel: PerAppProxyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadApps(this)
    }

    @Composable
    override fun ScreenContent() {
        val apps by viewModel.displayedApps.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val proxySet by viewModel.proxySet.collectAsStateWithLifecycle()
        val directSet by viewModel.directSet.collectAsStateWithLifecycle()
        val favorites by viewModel.favorites.collectAsStateWithLifecycle()
        val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
        val perAppProxyEnabled by viewModel.perAppProxyEnabled.collectAsStateWithLifecycle()
        val bypassApps by viewModel.bypassApps.collectAsStateWithLifecycle()

        PerAppProxyScreen(
            apps = apps,
            isLoading = isLoading,
            proxySet = proxySet,
            directSet = directSet,
            favorites = favorites,
            favoritesOnly = favoritesOnly,
            perAppProxyEnabled = perAppProxyEnabled,
            bypassApps = bypassApps,
            onBackClick = { finish() },
            onPerAppProxyChanged = { viewModel.setPerAppProxyEnabled(it) },
            onBypassAppsChanged = { viewModel.setBypassAppsEnabled(it) },
            onInfoClick = {
                toastInfo(R.string.summary_pref_per_app_proxy)
            },
            onAppModeChange = { packageName, mode -> viewModel.setAppMode(packageName, mode) },
            onFavoriteChange = { packageName, favorite -> viewModel.setFavorite(packageName, favorite) },
            onFavoritesOnlyChanged = { viewModel.setFavoritesOnly(it) },
            onSearch = { viewModel.filterApps(it) },
            onSelectAll = { viewModel.selectAll() },
            onInvertSelection = { viewModel.invertSelection() },
            onSelectProxyAuto = { viewModel.selectProxyAppAuto(this) },
            onImportProxyApp = {
                val content = Utils.getClipboard(applicationContext)
                viewModel.importProxyApp(content, this)
            },
            onExportProxyApp = {
                val export = viewModel.exportProxyApp()
                Utils.setClipboard(applicationContext, export)
                toastSuccess(R.string.toast_success)
            }
        )
    }
}

@Composable
fun PerAppProxyScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    proxySet: Set<String>,
    directSet: Set<String>,
    favorites: Set<String>,
    favoritesOnly: Boolean,
    perAppProxyEnabled: Boolean,
    bypassApps: Boolean,
    onBackClick: () -> Unit,
    onPerAppProxyChanged: (Boolean) -> Unit,
    onBypassAppsChanged: (Boolean) -> Unit,
    onInfoClick: () -> Unit,
    onAppModeChange: (String, PerAppMode) -> Unit,
    onFavoriteChange: (String, Boolean) -> Unit,
    onFavoritesOnlyChanged: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onSelectProxyAuto: () -> Unit,
    onImportProxyApp: () -> Unit,
    onExportProxyApp: () -> Unit
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        onSearch(searchQuery)
    }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.per_app_proxy_settings),
                onBackClick = onBackClick,
                isLoading = isLoading,
                isSearchActive = showSearch,
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    searchQuery = query
                    onSearch(query)
                },
                onSearchClose = {
                    searchQuery = ""
                    onSearch("")
                    showSearch = false
                },
                searchPlaceholder = stringResource(R.string.menu_item_search),
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                painterResource(R.drawable.ic_search_24dp),
                                contentDescription = stringResource(R.string.acc_search)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert_24dp),
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            AppDropdownMenuItems(PerAppMenuAction.entries, { it.labelRes }) { action ->
                                showMenu = false
                                when (action) {
                                    PerAppMenuAction.SelectAll -> onSelectAll()
                                    PerAppMenuAction.InvertSelection -> onInvertSelection()
                                    PerAppMenuAction.SelectProxyApps -> onSelectProxyAuto()
                                    PerAppMenuAction.ImportSelection -> onImportProxyApp()
                                    PerAppMenuAction.ExportSelection -> onExportProxyApp()
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.per_app_proxy_settings_enable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = perAppProxyEnabled,
                                modifier = Modifier.scale(0.65f),
                                onCheckedChange = onPerAppProxyChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                    checkedTrackColor = colorFabActive
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.switch_bypass_apps_mode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = bypassApps,
                                modifier = Modifier.scale(0.65f),
                                onCheckedChange = onBypassAppsChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                    checkedTrackColor = colorFabActive
                                )
                            )
                        }
                        IconButton(onClick = onInfoClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_about_24dp),
                                contentDescription = stringResource(R.string.acc_per_app_proxy_information),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFilterChip(
                            label = stringResource(R.string.per_app_focus_all),
                            selected = !favoritesOnly,
                            onClick = { onFavoritesOnlyChanged(false) }
                        )
                        AppFilterChip(
                            label = stringResource(R.string.per_app_focus_favorites),
                            selected = favoritesOnly,
                            onClick = { onFavoritesOnlyChanged(true) }
                        )
                    }
                }
            }
            AppDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(listState)
            ) {
                items(items = apps, key = { it.packageName }) { app ->
                    val mode = when {
                        app.packageName in directSet -> PerAppMode.DIRECT
                        app.packageName in proxySet -> PerAppMode.PROXY
                        else -> PerAppMode.DEFAULT
                    }
                    AppProxyItemRow(
                        appName = app.appName,
                        packageName = app.packageName,
                        mode = mode,
                        favorite = app.packageName in favorites,
                        onModeChange = { onAppModeChange(app.packageName, it) },
                        onFavoriteChange = { onFavoriteChange(app.packageName, it) }
                    )
                    ItemDivider()
                }
            }
        }
    }
}

@Composable
private fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) colorFabActive else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = background,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AppProxyItemRow(
    appName: String,
    packageName: String,
    mode: PerAppMode,
    favorite: Boolean,
    onModeChange: (PerAppMode) -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showModeMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val model = remember(packageName) {
            val data = "appicon:$packageName"
            ImageRequest.Builder(context)
                .data(data)
                .fetcherFactory(AppIconFetcher.Factory(context))
                .build()
        }
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            error = painterResource(R.drawable.ic_image_24dp),
            fallback = painterResource(R.drawable.ic_image_24dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { onFavoriteChange(!favorite) }) {
            Icon(
                painter = painterResource(
                    if (favorite) R.drawable.ic_star_24dp else R.drawable.ic_star_border_24dp
                ),
                contentDescription = stringResource(R.string.acc_per_app_favorite),
                tint = if (favorite) colorFabActive else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            Surface(
                onClick = { showModeMenu = true },
                color = modeColor(mode).copy(alpha = 0.15f),
                contentColor = modeColor(mode),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = stringResource(modeLabelRes(mode)),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            DropdownMenu(
                expanded = showModeMenu,
                onDismissRequest = { showModeMenu = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PerAppMode.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(stringResource(modeLabelRes(item))) },
                        onClick = {
                            showModeMenu = false
                            if (mode != item) {
                                onModeChange(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun modeColor(mode: PerAppMode): Color {
    return when (mode) {
        PerAppMode.PROXY -> colorFabActive
        PerAppMode.DIRECT -> MaterialTheme.colorScheme.error
        PerAppMode.DEFAULT -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@StringRes
private fun modeLabelRes(mode: PerAppMode): Int {
    return when (mode) {
        PerAppMode.PROXY -> R.string.per_app_mode_proxy
        PerAppMode.DIRECT -> R.string.per_app_mode_direct
        PerAppMode.DEFAULT -> R.string.per_app_mode_default
    }
}
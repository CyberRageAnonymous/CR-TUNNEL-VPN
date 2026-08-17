package com.cr.tunnel.ui.main

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.cr.tunnel.R
import com.cr.tunnel.ui.compose.AppTopBar
import com.cr.tunnel.ui.compose.MenuBottomSheet

@Composable
fun MainTopBar(
    isLoading: Boolean,
    showSearch: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onAction: (MainAction) -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit
) {
    var showImportMenu by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    AppTopBar(
        title = stringResource(R.string.title_server),
        onBackClick = {},
        isLoading = isLoading,
        isSearchActive = showSearch,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onSearchClose = onSearchClose,
        searchPlaceholder = stringResource(R.string.menu_item_search),
        navigationIcon = {
            if (showSearch) {
                IconButton(onClick = onSearchClose) {
                    Icon(painterResource(R.drawable.ic_arrow_back_24dp), contentDescription = stringResource(R.string.acc_back))
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(painterResource(R.drawable.ic_menu_24dp), contentDescription = stringResource(R.string.acc_open_menu))
                }
            }
        },
        actions = {
            if (!showSearch) {
                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(painterResource(R.drawable.ic_search_24dp), contentDescription = stringResource(R.string.acc_search))
                }
            }
            IconButton(onClick = { showImportMenu = true }) {
                Icon(painterResource(R.drawable.ic_add_24dp), contentDescription = stringResource(R.string.acc_add))
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(painterResource(R.drawable.ic_more_vert_24dp), contentDescription = stringResource(R.string.acc_more))
            }
        }
    )

    if (showImportMenu) {
        MenuBottomSheet(
            title = stringResource(R.string.menu_import_config),
            items = ImportMenuAction.entries,
            labelRes = { it.labelRes },
            iconRes = { it.iconRes },
            onSelected = { action ->
                showImportMenu = false
                onAction(action.action)
            },
            onDismiss = { showImportMenu = false }
        )
    }

    if (showMenu) {
        MenuBottomSheet(
            title = stringResource(R.string.menu_more),
            items = MainMoreMenuAction.entries,
            labelRes = { it.labelRes },
            iconRes = { it.iconRes },
            onSelected = { action ->
                showMenu = false
                onMoreMenuAction(action)
            },
            onDismiss = { showMenu = false }
        )
    }
}
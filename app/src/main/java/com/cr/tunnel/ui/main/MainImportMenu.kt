package com.cr.tunnel.ui.main

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cr.tunnel.R
import com.cr.tunnel.dto.entities.ProfileItem
import com.cr.tunnel.enums.EConfigType
import com.cr.tunnel.extension.isComplexType
import com.cr.tunnel.ui.compose.SelectListDialog

internal enum class ImportMenuAction(@StringRes val labelRes: Int, val action: MainAction, val iconRes: Int) {
    QRCode(R.string.menu_item_import_config_qrcode, MainAction.ImportQRcode, R.drawable.ic_qu_scan_24dp),
    Clipboard(R.string.menu_item_import_config_clipboard, MainAction.ImportClipboard, R.drawable.ic_copy),
    LocalFile(R.string.menu_item_import_config_local, MainAction.ImportConfigLocal, R.drawable.ic_file_24dp),
    PolicyGroup(R.string.menu_item_import_config_policy_group, MainAction.ImportManually(EConfigType.POLICYGROUP.value), R.drawable.ic_routing_24dp),
    ProxyChain(R.string.menu_item_import_config_proxy_chain, MainAction.ImportManually(EConfigType.PROXYCHAIN.value), R.drawable.ic_settings_24dp),
    Vmess(R.string.menu_item_import_config_manually_vmess, MainAction.ImportManually(EConfigType.VMESS.value), R.drawable.ic_description_24dp),
    Vless(R.string.menu_item_import_config_manually_vless, MainAction.ImportManually(EConfigType.VLESS.value), R.drawable.ic_description_24dp),
    Shadowsocks(R.string.menu_item_import_config_manually_ss, MainAction.ImportManually(EConfigType.SHADOWSOCKS.value), R.drawable.ic_description_24dp),
    Socks(R.string.menu_item_import_config_manually_socks, MainAction.ImportManually(EConfigType.SOCKS.value), R.drawable.ic_description_24dp),
    Http(R.string.menu_item_import_config_manually_http, MainAction.ImportManually(EConfigType.HTTP.value), R.drawable.ic_description_24dp),
    Trojan(R.string.menu_item_import_config_manually_trojan, MainAction.ImportManually(EConfigType.TROJAN.value), R.drawable.ic_description_24dp),
    WireGuard(R.string.menu_item_import_config_manually_wireguard, MainAction.ImportManually(EConfigType.WIREGUARD.value), R.drawable.ic_description_24dp),
    Hysteria2(R.string.menu_item_import_config_manually_hysteria2, MainAction.ImportManually(EConfigType.HYSTERIA2.value), R.drawable.ic_description_24dp)
}

enum class MainMoreMenuAction(@StringRes val labelRes: Int, val iconRes: Int) {
    AutoOptimize(R.string.menu_auto_optimize, R.drawable.ic_flash_on_24dp),
    RestartService(R.string.title_service_restart, R.drawable.ic_restore_24dp),
    DeleteAll(R.string.title_del_all_config, R.drawable.ic_delete_24dp),
    DeleteDuplicate(R.string.title_del_duplicate_config, R.drawable.ic_copy),
    DeleteInvalid(R.string.title_del_invalid_config, R.drawable.ic_outline_filter_alt_24),
    ExportAll(R.string.title_export_all, R.drawable.ic_backup_24dp),
    LocateSelected(R.string.title_locate_selected_config, R.drawable.ic_search_24dp),
    SortByTestResults(R.string.title_sort_by_test_results, R.drawable.ic_outline_filter_alt_24),
    TestAll(R.string.title_ping_all_server, R.drawable.ic_qu_start_24dp),
    TestAllRealPing(R.string.title_real_ping_all_server, R.drawable.ic_scan_24dp),
    UpdateSubscriptions(R.string.title_sub_update, R.drawable.ic_cloud_download_24dp)
}

internal enum class ServerMenuAction(
    @StringRes val labelRes: Int,
    val isShareAction: Boolean,
    val supportsComplexProfiles: Boolean,
) {
    ShareQRCode(R.string.share_method_qrcode, isShareAction = true, supportsComplexProfiles = false),
    ShareClipboard(R.string.share_method_clipboard, isShareAction = true, supportsComplexProfiles = false),
    ShareFullContent(R.string.share_method_full_content, isShareAction = true, supportsComplexProfiles = true),
    Edit(R.string.action_edit, isShareAction = false, supportsComplexProfiles = true),
    Delete(R.string.action_delete, isShareAction = false, supportsComplexProfiles = true),
}

internal fun serverMenuActions(
    isComplexProfile: Boolean,
    includeManagementActions: Boolean,
): List<ServerMenuAction> = ServerMenuAction.entries.filter { action ->
    (includeManagementActions || action.isShareAction) && (!isComplexProfile || action.supportsComplexProfiles)
}

@Composable
fun ShareMethodDialog(
    guid: String,
    profile: ProfileItem,
    more: Boolean,
    onDismiss: () -> Unit,
    onAction: (MainAction) -> Unit,
    onRemove: (String) -> Unit,
) {
    val menuActions = serverMenuActions(
        isComplexProfile = profile.configType.isComplexType(),
        includeManagementActions = more,
    )
    SelectListDialog(
        options = menuActions,
        optionText = { stringResource(it.labelRes) },
        onSelected = { action ->
            onDismiss()
            when (action) {
                ServerMenuAction.ShareQRCode -> onAction(MainAction.ShareQRCode(guid))
                ServerMenuAction.ShareClipboard -> onAction(MainAction.ShareClipboard(guid))
                ServerMenuAction.ShareFullContent -> onAction(MainAction.ShareFullContent(guid))
                ServerMenuAction.Edit -> onAction(MainAction.EditServer(guid, profile))
                ServerMenuAction.Delete -> onRemove(guid)
            }
        },
        onDismiss = onDismiss
    )
}

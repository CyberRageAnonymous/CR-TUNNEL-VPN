package com.cr.tunnel.ui.dnssettings

import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cr.tunnel.AppConfig
import com.cr.tunnel.R
import com.cr.tunnel.extension.toast
import com.cr.tunnel.ui.base.BaseComponentActivity
import com.cr.tunnel.ui.compose.AppDivider
import com.cr.tunnel.ui.compose.AppTopBar
import com.cr.tunnel.ui.compose.FormTextField
import com.cr.tunnel.ui.compose.PreferenceGroupHeader
import com.cr.tunnel.ui.compose.colorFabActive
import com.cr.tunnel.ui.compose.verticalScrollbar
import com.cr.tunnel.util.Utils

class DnsCustomActivity : BaseComponentActivity() {

    private val viewModel: DnsCustomViewModel by viewModels()

    @Composable
    override fun ScreenContent() {
        val remoteServers by viewModel.remoteServers.collectAsStateWithLifecycle()
        val domesticServers by viewModel.domesticServers.collectAsStateWithLifecycle()
        val dnsHosts by viewModel.dnsHosts.collectAsStateWithLifecycle()

        DnsCustomScreen(
            remoteServers = remoteServers,
            domesticServers = domesticServers,
            dnsHosts = dnsHosts,
            onBackClick = { finish() },
            onAddRemote = {
                if (validateServer(it)) viewModel.addRemoteServer(it)
            },
            onRemoveRemote = { viewModel.removeRemoteServer(it) },
            onAddDomestic = {
                if (validateServer(it)) viewModel.addDomesticServer(it)
            },
            onRemoveDomestic = { viewModel.removeDomesticServer(it) },
            onAddHost = { domain, address ->
                if (validateHost(domain, address)) viewModel.addHost(domain, address)
            },
            onRemoveHost = { viewModel.removeHost(it) }
        )
    }

    private fun validateServer(address: String): Boolean {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) {
            toast(R.string.dns_editor_invalid_dns)
            return false
        }
        if (!Utils.isPureIpAddress(trimmed) && !Utils.isCoreDNSAddress(trimmed)) {
            toast(R.string.dns_editor_invalid_dns)
            return false
        }
        return true
    }

    private fun validateHost(domain: String, address: String): Boolean {
        val trimmedDomain = domain.trim()
        val trimmedAddress = address.trim()
        if (trimmedDomain.isEmpty() || trimmedDomain.contains(",") || trimmedDomain.contains(":")) {
            toast(R.string.dns_editor_invalid_host)
            return false
        }
        if (trimmedAddress.isEmpty() || trimmedAddress.contains(",")) {
            toast(R.string.dns_editor_invalid_host)
            return false
        }
        return true
    }
}

@Composable
fun DnsCustomScreen(
    remoteServers: List<String>,
    domesticServers: List<String>,
    dnsHosts: List<DnsHostEntry>,
    onBackClick: () -> Unit,
    onAddRemote: (String) -> Unit,
    onRemoveRemote: (Int) -> Unit,
    onAddDomestic: (String) -> Unit,
    onRemoveDomestic: (Int) -> Unit,
    onAddHost: (String, String) -> Unit,
    onRemoveHost: (Int) -> Unit
) {
    var newRemote by rememberSaveable { mutableStateOf("") }
    var newDomestic by rememberSaveable { mutableStateOf("") }
    var newHostDomain by rememberSaveable { mutableStateOf("") }
    var newHostAddress by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_settings_dns_custom),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScrollbar(listState)
        ) {
            item(key = "remote_header") {
                PreferenceGroupHeader(stringResource(R.string.dns_editor_remote_servers))
            }
            item(key = "remote_presets") {
                DnsPresetChips { preset -> onAddRemote(preset) }
            }
            itemsIndexed(remoteServers, key = { index, _ -> "remote_$index" }) { index, server ->
                DnsEntryRow(value = server, onDelete = { onRemoveRemote(index) })
            }
            item(key = "remote_add") {
                DnsAddRow(
                    value = newRemote,
                    placeholder = stringResource(R.string.dns_editor_placeholder_server),
                    onValueChange = { newRemote = it },
                    onAdd = {
                        onAddRemote(newRemote)
                        newRemote = ""
                    }
                )
            }
            item(key = "divider1") {
                AppDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item(key = "domestic_header") {
                PreferenceGroupHeader(stringResource(R.string.dns_editor_domestic_servers))
            }
            item(key = "domestic_presets") {
                DnsPresetChips { preset -> onAddDomestic(preset) }
            }
            itemsIndexed(domesticServers, key = { index, _ -> "domestic_$index" }) { index, server ->
                DnsEntryRow(value = server, onDelete = { onRemoveDomestic(index) })
            }
            item(key = "domestic_add") {
                DnsAddRow(
                    value = newDomestic,
                    placeholder = stringResource(R.string.dns_editor_placeholder_server),
                    onValueChange = { newDomestic = it },
                    onAdd = {
                        onAddDomestic(newDomestic)
                        newDomestic = ""
                    }
                )
            }
            item(key = "divider2") {
                AppDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item(key = "hosts_header") {
                PreferenceGroupHeader(stringResource(R.string.dns_editor_dns_hosts))
            }
            itemsIndexed(dnsHosts, key = { index, item -> "host_${index}_${item.domain}" }) { index, host ->
                DnsEntryRow(
                    value = "${host.domain} → ${host.address}",
                    onDelete = { onRemoveHost(index) }
                )
            }
            item(key = "hosts_add") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormTextField(
                        label = stringResource(R.string.dns_editor_domain),
                        value = newHostDomain,
                        onValueChange = { newHostDomain = it },
                        modifier = Modifier.weight(1f)
                    )
                    FormTextField(
                        label = stringResource(R.string.dns_editor_address),
                        value = newHostAddress,
                        onValueChange = { newHostAddress = it },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            onAddHost(newHostDomain, newHostAddress)
                            if (newHostDomain.isNotBlank() && newHostAddress.isNotBlank()) {
                                newHostDomain = ""
                                newHostAddress = ""
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_add_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.dns_editor_add_host))
                    }
                }
            }
            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DnsPresetChips(onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppConfig.DNS_PRESET_SERVERS.take(6).forEach { preset ->
            Surface(
                color = colorFabActive.copy(alpha = 0.15f),
                contentColor = colorFabActive,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.clickable { onSelect(preset) }
            ) {
                Text(
                    text = preset,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun DnsEntryRow(
    value: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(
                painterResource(R.drawable.ic_delete_24dp),
                contentDescription = stringResource(R.string.acc_delete),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DnsAddRow(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        FormTextField(
            label = stringResource(R.string.dns_editor_add_server),
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onAdd,
            modifier = Modifier.padding(end = 8.dp, top = 4.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_add_24dp),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.dns_editor_add))
        }
    }
}
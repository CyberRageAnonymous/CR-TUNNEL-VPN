package com.cr.tunnel.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cr.tunnel.handler.MmkvManager

object AdvancedServerSettingsStore {
    fun getBool(key: String, def: Boolean): Boolean =
        MmkvManager.decodeSettingsBool("adv_$key", def)

    fun setBool(key: String, v: Boolean) = MmkvManager.encodeSettings("adv_$key", v)

    fun getStr(key: String): String =
        MmkvManager.decodeSettingsString("adv_$key", "") ?: ""

    fun setStr(key: String, v: String) = MmkvManager.encodeSettings("adv_$key", v)
}

@Composable
fun AdvancedServerSettings(onDismiss: () -> Unit) {
    val scroll = rememberScrollState()
    var mux by remember { mutableStateOf(AdvancedServerSettingsStore.getBool("mux", true)) }
    var sniffing by remember { mutableStateOf(AdvancedServerSettingsStore.getBool("sniffing", false)) }
    var decoy by remember { mutableStateOf(AdvancedServerSettingsStore.getBool("decoy", true)) }
    var udp by remember { mutableStateOf(AdvancedServerSettingsStore.getBool("udp", false)) }
    var shuffleSni by remember { mutableStateOf(AdvancedServerSettingsStore.getBool("shuffle_sni", false)) }
    var routing by remember { mutableStateOf(AdvancedServerSettingsStore.getBool("routing", true)) }

    var dnsServer by remember { mutableStateOf(AdvancedServerSettingsStore.getStr("dns_server")) }
    var dnsPort by remember { mutableStateOf(AdvancedServerSettingsStore.getStr("dns_port")) }
    var routingRules by remember { mutableStateOf(AdvancedServerSettingsStore.getStr("routing_rules")) }
    var udpShaping by remember { mutableStateOf(AdvancedServerSettingsStore.getStr("udp_shaping")) }

    fun save() {
        AdvancedServerSettingsStore.setBool("mux", mux)
        AdvancedServerSettingsStore.setBool("sniffing", sniffing)
        AdvancedServerSettingsStore.setBool("decoy", decoy)
        AdvancedServerSettingsStore.setBool("udp", udp)
        AdvancedServerSettingsStore.setBool("shuffle_sni", shuffleSni)
        AdvancedServerSettingsStore.setBool("routing", routing)
        AdvancedServerSettingsStore.setStr("dns_server", dnsServer)
        AdvancedServerSettingsStore.setStr("dns_port", dnsPort)
        AdvancedServerSettingsStore.setStr("routing_rules", routingRules)
        AdvancedServerSettingsStore.setStr("udp_shaping", udpShaping)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text(
            text = "Advanced Server Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        @Composable fun settingRow(title: String, desc: String? = null, value: @Composable () -> Unit) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    desc?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                }
                value()
            }
        }

        settingRow(
            title = "MUX (multiplexing)",
            desc = "Aggregate multiple TCP connections over one stream",
            value = { Switch(checked = mux, onCheckedChange = { mux = it }) }
        )
        settingRow(
            title = "Sniffing (destination sniffing)",
            desc = "Detect destination from SNI/HTTP and route accordingly",
            value = { Switch(checked = sniffing, onCheckedChange = { sniffing = it }) }
        )
        settingRow(
            title = "Decoy (disguise as HTTPS)",
            desc = "Disguise traffic as normal HTTPS when obfuscation needed",
            value = { Switch(checked = decoy, onCheckedChange = { decoy = it }) }
        )
        settingRow(
            title = "UDP over stream",
            desc = "Force UDP traffic over the tunnel stream",
            value = { Switch(checked = udp, onCheckedChange = { udp = it }) }
        )
        settingRow(
            title = "Anti-DPI: shuffle SNI",
            desc = "Randomize SNI in ClientHello to evade DPI systems",
            value = { Switch(checked = shuffleSni, onCheckedChange = { shuffleSni = it }) }
        )
        settingRow(
            title = "Routing rules",
            desc = "Enable custom routing rules for this server",
            value = { Switch(checked = routing, onCheckedChange = { routing = it }) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("DNS Server", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = dnsServer,
            onValueChange = { dnsServer = it; AdvancedServerSettingsStore.setStr("dns_server", it) },
            placeholder = { Text("e.g. 1.1.1.1 or https://dns.google/dns-query") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dnsPort,
            onValueChange = { dnsPort = it; AdvancedServerSettingsStore.setStr("dns_port", it) },
            placeholder = { Text("e.g. 53 / 853 / 443") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("Routing rules (one per line: domain -> outbound)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = routingRules,
            onValueChange = { routingRules = it; AdvancedServerSettingsStore.setStr("routing_rules", it) },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("Anti-DPI: UDP shaping", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = udpShaping,
            onValueChange = { udpShaping = it; AdvancedServerSettingsStore.setStr("udp_shaping", it) },
            placeholder = { Text("e.g. bucket limit / padding") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { save(); onDismiss() }, modifier = Modifier.align(Alignment.End)) {
            Text("Save", fontWeight = FontWeight.SemiBold)
        }
    }
}

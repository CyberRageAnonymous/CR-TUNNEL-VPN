package com.cr.tunnel.ui.community

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cr.tunnel.R
import com.cr.tunnel.dto.CommunityConfigItem
import com.cr.tunnel.extension.toastSuccess
import com.cr.tunnel.ui.base.BaseComponentActivity
import com.cr.tunnel.ui.compose.AppTopBar

class CommunityConfigActivity : BaseComponentActivity() {

    private val viewModel: CommunityViewModel by viewModels()

    @Composable
    override fun ScreenContent() {
        CommunityScreen(viewModel = viewModel, onBackClick = { finish() })
    }
}

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.community_title),
                onBackClick = onBackClick,
                isLoading = uiState.loading
            )
        },
        bottomBar = {
            ShareButton(onClick = { viewModel.startSharing() })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.loading && uiState.rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.community_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.rows, key = { it.config.id }) { row ->
                        CommunityCard(
                            row = row,
                            onCopy = {
                                copyToClipboard(context, row.config.link)
                                context.toastSuccess(R.string.community_copied)
                            },
                            onPing = { viewModel.pingRow(row.config.id) }
                        )
                    }
                }
            }
        }

        uiState.shareStep?.let { step ->
            ShareStepDialog(
                step = step,
                draft = uiState.draft,
                submitting = uiState.submitting,
                onNext = { viewModel.nextShareStep(it) },
                onLinkChange = { viewModel.updateLinkInput(it) },
                onCancel = { viewModel.cancelSharing() }
            )
        }
    }
}

@Composable
private fun ShareButton(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = stringResource(R.string.community_share_config))
        }
    }
}

@Composable
private fun CommunityCard(
    row: CommunityRow,
    onCopy: () -> Unit,
    onPing: () -> Unit
) {
    val config = row.config
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProtocolChip(link = config.link)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = config.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            PingText(row = row)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetaText(label = stringResource(R.string.community_label_volume), value = config.volume, modifier = Modifier.weight(1f))
            MetaText(label = stringResource(R.string.community_label_duration), value = config.duration, modifier = Modifier.weight(1f))
            MetaText(label = stringResource(R.string.community_label_users), value = config.users, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onCopy, shape = RoundedCornerShape(10.dp)) {
                Text(stringResource(R.string.community_copy))
            }
            OutlinedButton(
                onClick = onPing,
                enabled = !row.isPinging,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (row.isPinging) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(stringResource(R.string.community_ping))
            }
        }
    }
}

@Composable
private fun ProtocolChip(link: String) {
    val protocol = link.substringBefore("://").uppercase().ifBlank { "?" }
    Box(
        modifier = Modifier
            .background(Color(0x3300E5FF), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = protocol.take(9),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00B8D4),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PingText(row: CommunityRow) {
    val color = when {
        row.isInvalid -> Color(0xFFF44336)
        row.pingText.endsWith("ms") -> Color(0xFF43A047)
        row.pingText.isNotEmpty() -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = when {
            row.isPinging -> stringResource(R.string.connection_test_testing)
            row.isInvalid -> stringResource(R.string.connection_test_fail)
            else -> row.pingText.ifEmpty { "" }
        },
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1
    )
}

@Composable
private fun MetaText(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShareStepDialog(
    step: ShareStep,
    draft: ShareDraft,
    submitting: Boolean,
    onNext: (String) -> Unit,
    onLinkChange: (String) -> Unit,
    onCancel: () -> Unit
) {
    var input by remember(step) { mutableStateOf(if (step == ShareStep.LINK) draft.link else "") }
    val titleRes = when (step) {
        ShareStep.VOLUME -> R.string.community_q_volume
        ShareStep.DURATION -> R.string.community_q_duration
        ShareStep.USERS -> R.string.community_q_users
        ShareStep.LINK -> R.string.community_paste_link
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(titleRes)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = step != ShareStep.LINK,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == ShareStep.LINK) {
                        onLinkChange(input)
                        onNext(input)
                    } else {
                        onNext(input)
                    }
                },
                enabled = !submitting && input.isNotBlank()
            ) {
                Text(
                    stringResource(
                        if (step == ShareStep.LINK) R.string.community_publish else R.string.community_next
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !submitting) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun copyToClipboard(context: Context, link: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("config", link))
}

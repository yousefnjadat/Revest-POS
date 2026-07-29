package com.example.pos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.pos.domain.Order
import com.example.pos.domain.OrderSyncState
import com.example.pos.ui.theme.statusColors

@Composable
fun SyncStatusChip(
    order: Order,
    modifier: Modifier = Modifier,
) {
    when (order.syncState) {
        OrderSyncState.SYNCED ->
            LabelChip(
                text = "Synced",
                modifier = modifier,
                icon = Icons.Outlined.CloudDone,
                container = MaterialTheme.statusColors.successContainer,
                content = MaterialTheme.statusColors.onSuccessContainer,
                contentDescription = "Status: synced",
            )

        OrderSyncState.SYNCING ->
            LabelChip(
                text = "Syncing",
                modifier = modifier,
                icon = Icons.Outlined.Sync,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                contentDescription = "Status: syncing",
            )

        OrderSyncState.FAILED ->
            LabelChip(
                text = "Retry needed",
                modifier = modifier,
                icon = Icons.Outlined.ErrorOutline,
                container = MaterialTheme.statusColors.warningContainer,
                content = MaterialTheme.statusColors.onWarningContainer,
                contentDescription = "Status: retry needed, saved on this device",
            )

        OrderSyncState.PENDING ->
            LabelChip(
                text = "Saved, pending",
                modifier = modifier,
                icon = Icons.Outlined.Schedule,
                contentDescription = "Status: saved on this device, waiting to sync",
            )
    }
}

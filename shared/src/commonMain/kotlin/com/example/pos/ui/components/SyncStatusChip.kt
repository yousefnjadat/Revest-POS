package com.example.pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.pos.domain.Order
import com.example.pos.domain.OrderSyncState
import com.example.pos.ui.theme.PosSpacing
import com.example.pos.ui.theme.statusColors

/**
 * Sync state of one order. Every state has its own icon *and* its own words, so the chip is
 * still readable in greyscale or with colour vision differences.
 */
@Composable
fun SyncStatusChip(
    order: Order,
    modifier: Modifier = Modifier,
) {
    val failed = order.syncState == OrderSyncState.FAILED
    val icon: ImageVector
    val label: String
    val container: Color
    val content: Color

    when {
        order.syncState == OrderSyncState.SYNCED -> {
            icon = Icons.Outlined.CloudDone
            label = "Synced"
            container = MaterialTheme.statusColors.successContainer
            content = MaterialTheme.statusColors.onSuccessContainer
        }

        order.syncState == OrderSyncState.SYNCING -> {
            icon = Icons.Outlined.Sync
            label = "Syncing"
            container = MaterialTheme.colorScheme.primaryContainer
            content = MaterialTheme.colorScheme.onPrimaryContainer
        }

        failed -> {
            icon = Icons.Outlined.ErrorOutline
            label = "Retry needed"
            container = MaterialTheme.statusColors.warningContainer
            content = MaterialTheme.statusColors.onWarningContainer
        }

        else -> {
            icon = Icons.Outlined.Schedule
            label = "Pending"
            container = MaterialTheme.colorScheme.surfaceContainerHighest
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = "Status: $label" },
        shape = RoundedCornerShape(percent = 50),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.sm, vertical = PosSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

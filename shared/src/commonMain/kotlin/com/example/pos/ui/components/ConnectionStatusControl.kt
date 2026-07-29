package com.example.pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.pos.ui.theme.PosSpacing
import com.example.pos.ui.theme.statusColors

/**
 * The online/offline toggle in the app bar.
 *
 * Status is carried three ways — a distinct icon, a written label, and colour — so it never
 * depends on colour alone. The whole pill is one 48dp-high tap target, and screen readers
 * announce it as a switch with its current state.
 */
@Composable
fun ConnectionStatusControl(
    isOnline: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container =
        if (isOnline) {
            MaterialTheme.statusColors.successContainer
        } else {
            MaterialTheme.statusColors.warningContainer
        }
    val content =
        if (isOnline) {
            MaterialTheme.statusColors.onSuccessContainer
        } else {
            MaterialTheme.statusColors.onWarningContainer
        }
    val label = if (isOnline) "Online" else "Offline"

    Surface(
        onClick = { onToggle(!isOnline) },
        modifier =
            modifier
                .defaultMinSize(minHeight = PosSpacing.touchTarget)
                .semantics(mergeDescendants = true) {
                    role = Role.Switch
                    contentDescription = "Connection"
                    stateDescription = if (isOnline) "Online" else "Offline"
                },
        shape = RoundedCornerShape(percent = 50),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

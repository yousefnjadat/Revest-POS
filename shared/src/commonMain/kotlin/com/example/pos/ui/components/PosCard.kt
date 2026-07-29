package com.example.pos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pos.ui.theme.PosSpacing

/**
 * The one card in this app: 16dp corners, a hairline outline, no elevation.
 *
 * A single container keeps product, cart, order, and summary cards identical, and flat surfaces
 * with a border read cleaner on a retail screen than stacked shadows.
 */
@Composable
fun PosCard(
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    contentPadding: androidx.compose.ui.unit.Dp = PosSpacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color =
            if (muted) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            content = content,
        )
    }
}

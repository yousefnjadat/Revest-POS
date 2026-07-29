package com.example.pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.pos.ui.theme.PosSpacing
import com.example.pos.ui.theme.statusColors

enum class BannerTone { Info, Warning }

/**
 * The one inline notice: offline checkout, a sync that needs retrying, anything the cashier
 * should read but not be blocked by. Tinted containers only — never a saturated fill — so a
 * banner sits calmly inside a card instead of shouting.
 */
@Composable
fun PosBanner(
    tone: BannerTone,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container =
        when (tone) {
            BannerTone.Info -> MaterialTheme.colorScheme.surfaceContainerHigh
            BannerTone.Warning -> MaterialTheme.statusColors.warningContainer
        }
    val onContainer =
        when (tone) {
            BannerTone.Info -> MaterialTheme.colorScheme.onSurfaceVariant
            BannerTone.Warning -> MaterialTheme.statusColors.onWarningContainer
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = onContainer,
    ) {
        Row(
            modifier = Modifier.padding(PosSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(top = 1.dp).size(16.dp),
            )
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelMedium,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                    content = content,
                )
            }
        }
    }
}

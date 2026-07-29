package com.example.pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pos.ui.theme.PosSpacing

/**
 * Compact minus / quantity / plus control.
 *
 * Both buttons are full 48dp targets even though the icons are small, and the minus turns into a
 * delete icon at one unit so the last tap reads as "remove", not "go to zero".
 */
@Composable
fun QuantityStepper(
    quantity: Int,
    productName: String,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = PosSpacing.touchTarget),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onDecrease,
                modifier = Modifier.size(PosSpacing.touchTarget),
                colors =
                    IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            ) {
                Icon(
                    imageVector = if (quantity > 1) Icons.Filled.Remove else Icons.Filled.Delete,
                    contentDescription =
                        if (quantity > 1) {
                            "Remove one $productName"
                        } else {
                            "Remove $productName from the cart"
                        },
                    modifier = Modifier.size(18.dp),
                )
            }

            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 24.dp),
            )

            IconButton(
                onClick = onIncrease,
                enabled = canIncrease,
                modifier = Modifier.size(PosSpacing.touchTarget),
                colors =
                    IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add one more $productName",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

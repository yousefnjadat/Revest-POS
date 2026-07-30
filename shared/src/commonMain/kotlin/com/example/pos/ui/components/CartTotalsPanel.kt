package com.example.pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pos.domain.CartCalculator
import com.example.pos.domain.model.CartTotals
import com.example.pos.ui.formatMoney
import com.example.pos.ui.theme.PosSpacing
import com.example.pos.ui.theme.statusColors


@Composable
fun CartTotalsPanel(
    totals: CartTotals,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
    ) {
        TotalsRow(label = "Subtotal", cents = totals.subtotalCents)
        TotalsRow(
            label = "Tax (${CartCalculator.TAX_PERCENT}% of taxable items)",
            cents = totals.taxCents,
        )

        if (totals.discountApplied) {
            TotalsRow(
                label = "Discount (${CartCalculator.DISCOUNT_PERCENT}%)",
                cents = totals.discountCents,
                prefix = "-",
                color = MaterialTheme.statusColors.success,
            )
        } else if (totals.subtotalCents > 0) {
            DiscountHint(
                remainingCents = CartCalculator.DISCOUNT_THRESHOLD_CENTS - totals.subtotalCents,
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = PosSpacing.sm),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Total", style = MaterialTheme.typography.titleMedium)
            MoneyText(
                cents = totals.totalCents,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TotalsRow(
    label: String,
    cents: Long,
    prefix: String = "",
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = color)
        MoneyText(
            cents = cents,
            prefix = prefix,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

@Composable
private fun DiscountHint(remainingCents: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Savings,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
                "${formatMoney(remainingCents)} more to earn " +
                        "${CartCalculator.DISCOUNT_PERCENT}% off",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

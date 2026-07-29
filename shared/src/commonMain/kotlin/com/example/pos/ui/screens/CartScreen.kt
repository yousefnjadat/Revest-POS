package com.example.pos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.components.PlaceholderPanel
import com.example.pos.ui.formatMoney
import com.example.pos.ui.theme.PosSpacing

/** Placeholder cart. Quantity steppers and the totals panel arrive in the next phase. */
@Composable
fun CartScreen(
    state: PosUiState,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
    ) {
        if (state.cartLines.isEmpty()) {
            PlaceholderPanel(title = "Cart is empty") {
                Text(
                    text = "Add products from the catalog to start a sale.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        state.cartLines.forEach { line ->
            PlaceholderPanel(title = line.product.name) {
                Text(
                    text =
                        "${line.quantity} x ${formatMoney(line.product.priceCents)} = " +
                            formatMoney(line.lineTotalCents),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
                    OutlinedButton(onClick = { onDecrease(line.product.id) }) { Text("-") }
                    OutlinedButton(onClick = { onIncrease(line.product.id) }) { Text("+") }
                    TextButton(onClick = { onRemove(line.product.id) }) { Text("Remove") }
                }
            }
        }

        val totals = state.totals
        PlaceholderPanel(title = "Totals") {
            TotalsRow("Subtotal", formatMoney(totals.subtotalCents))
            TotalsRow("Tax (10% of taxable)", formatMoney(totals.taxCents))
            if (totals.discountApplied) {
                TotalsRow("Discount (5%)", "-" + formatMoney(totals.discountCents))
            }
            TotalsRow("Total", formatMoney(totals.totalCents), emphasise = true)
            Button(
                onClick = onCheckout,
                enabled = !state.isCheckingOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isCheckingOut) "Saving..." else "Checkout")
            }
        }
    }
}

@Composable
private fun TotalsRow(label: String, value: String, emphasise: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style =
                if (emphasise) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
            color =
                if (emphasise) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Text(
            text = value,
            style =
                if (emphasise) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
        )
    }
}

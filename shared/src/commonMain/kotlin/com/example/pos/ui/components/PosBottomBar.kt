package com.example.pos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.pos.presentation.AppDestination

@Composable
fun PosBottomBar(
    current: AppDestination,
    cartItemCount: Int,
    pendingOrderCount: Int,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        AppDestination.entries.forEach { destination ->
            val selected = destination == current
            val badgeCount = destination.badgeCount(cartItemCount, pendingOrderCount)

            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge { Text(badgeCount.coerceAtMost(99).toString()) }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = destination.icon(selected),
                            contentDescription = null,
                        )
                    }
                },
                label = { Text(destination.label) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}

/** Cart shows what is in it; Orders shows what still has to reach the backend. */
private fun AppDestination.badgeCount(cartItemCount: Int, pendingOrderCount: Int): Int =
    when (this) {
        AppDestination.CATALOG -> 0
        AppDestination.CART -> cartItemCount
        AppDestination.ORDERS -> pendingOrderCount
    }

private fun AppDestination.icon(selected: Boolean): ImageVector =
    when (this) {
        AppDestination.CATALOG ->
            if (selected) Icons.Filled.Storefront else Icons.Outlined.Storefront

        AppDestination.CART ->
            if (selected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart

        AppDestination.ORDERS ->
            if (selected) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong
    }

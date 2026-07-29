package com.example.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pos.domain.Product
import com.example.pos.presentation.CatalogUiState
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.components.ProductCard
import com.example.pos.ui.formatMoney
import com.example.pos.ui.theme.PosSpacing

/** Below this width a two-column grid squeezes product names into unreadable stacks. */
private val GRID_MIN_WIDTH = 360.dp

@Composable
fun CatalogScreen(
    state: PosUiState,
    onAddProduct: (String) -> Unit,
    onDecreaseProduct: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        when (val catalog = state.catalog) {
            is CatalogUiState.Loading ->
                CatalogSkeleton(modifier = Modifier.weight(1f))

            is CatalogUiState.Empty ->
                CatalogNotice(
                    icon = Icons.Outlined.Inventory2,
                    title = "No products yet",
                    body = "The catalog came back empty. Reload once stock has been published.",
                    actionLabel = "Reload",
                    onAction = onRetry,
                    modifier = Modifier.weight(1f),
                )

            is CatalogUiState.Error ->
                CatalogNotice(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't load the catalog",
                    body = catalog.message,
                    actionLabel = "Try again",
                    onAction = onRetry,
                    isError = true,
                    modifier = Modifier.weight(1f),
                )

            is CatalogUiState.Content -> {
                CatalogHeader(
                    availableCount = catalog.products.count { it.inStock },
                    modifier = Modifier.padding(horizontal = PosSpacing.screenHorizontal),
                )
                ProductCollection(
                    products = catalog.products,
                    quantityOf = state.cart::quantityOf,
                    onAddProduct = onAddProduct,
                    onDecreaseProduct = onDecreaseProduct,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        CartSummaryBar(itemCount = state.cartItemCount, totalCents = state.totals.totalCents)
    }
}

@Composable
private fun CatalogHeader(availableCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = PosSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Products", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "$availableCount available",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProductCollection(
    products: List<Product>,
    quantityOf: (String) -> Int,
    onAddProduct: (String) -> Unit,
    onDecreaseProduct: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val contentPadding =
            PaddingValues(
                start = PosSpacing.screenHorizontal,
                end = PosSpacing.screenHorizontal,
                bottom = PosSpacing.md,
            )

        if (maxWidth >= GRID_MIN_WIDTH) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                items(items = products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        quantityInCart = quantityOf(product.id),
                        onAdd = { onAddProduct(product.id) },
                        onDecrease = { onDecreaseProduct(product.id) },
                    )
                }
            }
        } else {
            // Too narrow for two readable columns — one card per row instead.
            LazyColumn(
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
            ) {
                items(items = products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        quantityInCart = quantityOf(product.id),
                        onAdd = { onAddProduct(product.id) },
                        onDecrease = { onDecreaseProduct(product.id) },
                    )
                }
            }
        }
    }
}

/** Static placeholder cards. No shimmer — a still skeleton reads as loading without flicker. */
@Composable
private fun CatalogSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = PosSpacing.screenHorizontal, vertical = PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Text(
            text = "Loading products...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = PosSpacing.sm),
        )
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm)) {
                SkeletonCard(modifier = Modifier.weight(1f))
                SkeletonCard(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(172.dp),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(PosSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            SkeletonBlock(width = 40.dp, height = 40.dp)
            SkeletonBlock(width = 120.dp, height = 14.dp)
            SkeletonBlock(width = 72.dp, height = 14.dp)
            SkeletonBlock(width = 148.dp, height = 36.dp)
        }
    }
}

@Composable
private fun SkeletonBlock(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(width = width, height = height),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = {},
    )
}

@Composable
private fun CatalogNotice(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(PosSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onAction,
                modifier = Modifier.height(PosSpacing.touchTarget).padding(top = PosSpacing.xs),
            ) {
                Text(actionLabel)
            }
        }
    }
}

/** Sits directly above the bottom bar, so the running total is visible while browsing. */
@Composable
private fun CartSummaryBar(itemCount: Int, totalCents: Long) {
    AnimatedVisibility(
        visible = itemCount > 0,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = PosSpacing.screenHorizontal,
                                vertical = PosSpacing.sm,
                            ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (itemCount == 1) "1 item in cart" else "$itemCount items in cart",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMoney(totalCents),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

package com.example.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pos.domain.Product
import com.example.pos.presentation.CatalogUiState
import com.example.pos.presentation.PosUiState
import com.example.pos.ui.components.EmptyState
import com.example.pos.ui.components.MoneyText
import com.example.pos.ui.components.PosCard
import com.example.pos.ui.components.ProductCard
import com.example.pos.ui.components.ScreenHeader
import com.example.pos.ui.theme.PosSpacing

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
            is CatalogUiState.Loading -> {
                ScreenHeader(title = "Products")
                CatalogSkeleton(modifier = Modifier.weight(1f))
            }

            is CatalogUiState.Empty ->
                EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = "No products yet",
                    message = "The catalog came back empty. Reload once stock has been published.",
                    actionLabel = "Reload",
                    onAction = onRetry,
                    modifier = Modifier.weight(1f),
                )

            is CatalogUiState.Error ->
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Couldn't load the catalog",
                    message = catalog.message,
                    isError = true,
                    actionLabel = "Try again",
                    onAction = onRetry,
                    modifier = Modifier.weight(1f),
                )

            is CatalogUiState.Content -> {
                ScreenHeader(
                    title = "Products",
                    trailing = "${catalog.products.count { it.inStock }} available",
                )
                ProductCollection(
                    products = catalog.products,
                    quantityOf = state.cart::quantityOf,
                    isRefreshing = state.catalog == CatalogUiState.Loading,
                    onRefresh = onRetry,
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
private fun ProductCollection(
    products: List<Product>,
    quantityOf: (String) -> Int,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
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
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier
            ) {
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
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier
            ) {
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
}

@Composable
private fun CatalogSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = PosSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
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
    PosCard(modifier = modifier) {
        SkeletonBlock(width = 40.dp, height = 40.dp)
        SkeletonBlock(width = 120.dp, height = 14.dp)
        SkeletonBlock(width = 72.dp, height = 14.dp)
        SkeletonBlock(width = 148.dp, height = PosSpacing.touchTarget)
    }
}

@Composable
private fun SkeletonBlock(width: Dp, height: Dp) {
    Surface(
        modifier = Modifier.size(width = width, height = height),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = {},
    )
}

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
                            .height(PosSpacing.touchTarget)
                            .padding(horizontal = PosSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (itemCount == 1) "1 item in cart" else "$itemCount items in cart",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MoneyText(
                        cents = totalCents,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

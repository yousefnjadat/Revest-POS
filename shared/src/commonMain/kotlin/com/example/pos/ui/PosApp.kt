package com.example.pos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pos.presentation.AppDestination
import com.example.pos.presentation.PosViewModel
import com.example.pos.ui.components.PosBottomBar
import com.example.pos.ui.components.PosTopBar
import com.example.pos.ui.screens.CartScreen
import com.example.pos.ui.screens.CatalogScreen
import com.example.pos.ui.screens.OrdersScreen
import androidx.compose.ui.unit.dp
import com.example.pos.ui.theme.PosTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * The application shell: one scaffold, one view model, and a `when` over the destination held in
 * state. No navigation library — three destinations do not need a back stack.
 */
@Composable
fun PosApp(viewModel: PosViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message = message.text, withDismissAction = true)
        }
    }

    PosTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    PosTopBar(isOnline = state.isOnline, onToggleOnline = viewModel::setOnline)
                    // A 2dp bar rather than a spinner: progress belongs to the whole app here.
                    AnimatedVisibility(
                        visible = state.isSyncing,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            bottomBar = {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    PosBottomBar(
                        current = state.destination,
                        cartItemCount = state.cartItemCount,
                        pendingOrderCount = state.pendingOrderCount,
                        onSelect = viewModel::selectDestination,
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { contentPadding ->
            // Each screen owns its own scrolling, so lazy lists and grids keep bounded heights.
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                when (state.destination) {
                    AppDestination.CATALOG ->
                        CatalogScreen(
                            state = state,
                            onAddProduct = viewModel::addProduct,
                            onDecreaseProduct = viewModel::decreaseProduct,
                            onRetry = viewModel::retryCatalog,
                        )

                    AppDestination.CART ->
                        CartScreen(
                            state = state,
                            onIncrease = viewModel::addProduct,
                            onDecrease = viewModel::decreaseProduct,
                            onRemove = viewModel::removeProduct,
                            onCheckout = viewModel::checkout,
                            onBrowseCatalog = {
                                viewModel.selectDestination(AppDestination.CATALOG)
                            },
                        )

                    AppDestination.ORDERS ->
                        OrdersScreen(state = state, onSyncNow = viewModel::syncNow)
                }
            }
        }
    }
}

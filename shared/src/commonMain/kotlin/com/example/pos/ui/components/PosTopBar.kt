package com.example.pos.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.pos.APP_NAME
import com.example.pos.ui.theme.PosSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosTopBar(
    isOnline: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = APP_NAME, style = MaterialTheme.typography.titleLarge) },
        actions = {
            ConnectionStatusControl(
                isOnline = isOnline,
                onToggle = onToggleOnline,
                modifier = Modifier.padding(end = PosSpacing.md),
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

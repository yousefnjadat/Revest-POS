package com.example.pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.pos.ui.theme.PosSpacing

/**
 * Title on the left, one supporting figure on the right. Deliberately `titleLarge` rather than a
 * display size — this is a working till, not a landing page, and the screen title should not
 * out-shout the totals.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PosSpacing.screenHorizontal,
                    vertical = PosSpacing.md,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

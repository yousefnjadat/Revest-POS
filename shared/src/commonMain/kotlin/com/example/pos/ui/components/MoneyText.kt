package com.example.pos.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.pos.ui.formatMoney

/**
 * Every price in the app goes through here.
 *
 * Prices are rendered with tabular figures (`tnum`), so digits share one width and totals line up
 * column-wise down the cart and the order list instead of wobbling.
 */
@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    prefix: String = "",
) {
    Text(
        text = prefix + formatMoney(cents),
        modifier = modifier,
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
        maxLines = 1,
    )
}

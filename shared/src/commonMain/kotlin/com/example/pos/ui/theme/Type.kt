package com.example.pos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val PosTypography =
    Typography().run {
        copy(
            headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
            titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
            titleSmall = titleSmall.copy(fontWeight = FontWeight.Medium),
            bodyMedium = bodyMedium.copy(lineHeight = 20.sp),
            labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
            labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium),
        )
    }

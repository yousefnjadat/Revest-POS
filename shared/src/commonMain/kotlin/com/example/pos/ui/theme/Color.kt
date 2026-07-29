package com.example.pos.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val PosLightColors =
    lightColorScheme(
        primary = Color(0xFF3157E8),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE8EDFF),
        onPrimaryContainer = Color(0xFF001449),
        secondary = Color(0xFF505A72),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDCE2F4),
        onSecondaryContainer = Color(0xFF121B2C),
        tertiary = Color(0xFF3E5F8A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD5E3FF),
        onTertiaryContainer = Color(0xFF001C39),
        background = Color(0xFFF6F7FB),
        onBackground = Color(0xFF172033),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF172033),
        surfaceVariant = Color(0xFFEEF1F6),
        onSurfaceVariant = Color(0xFF454C5C),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFBFCFE),
        surfaceContainer = Color(0xFFF2F4F9),
        surfaceContainerHigh = Color(0xFFEEF1F6),
        surfaceContainerHighest = Color(0xFFE7EBF2),
        outline = Color(0xFF737A8A),
        outlineVariant = Color(0xFFD5DAE3),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFCE1DE),
        onErrorContainer = Color(0xFF410E0B),
        scrim = Color(0xFF000000),
    )

internal val PosDarkColors =
    darkColorScheme(
        primary = Color(0xFFB4C5FF),
        onPrimary = Color(0xFF002B76),
        primaryContainer = Color(0xFF1F45A6),
        onPrimaryContainer = Color(0xFFDBE1FF),
        secondary = Color(0xFFB9C3DC),
        onSecondary = Color(0xFF243042),
        secondaryContainer = Color(0xFF3A4559),
        onSecondaryContainer = Color(0xFFD5DEF5),
        tertiary = Color(0xFFA7C8FF),
        onTertiary = Color(0xFF00325C),
        tertiaryContainer = Color(0xFF204876),
        onTertiaryContainer = Color(0xFFD5E3FF),
        background = Color(0xFF11151E),
        onBackground = Color(0xFFE3E6ED),
        surface = Color(0xFF11151E),
        onSurface = Color(0xFFE3E6ED),
        surfaceVariant = Color(0xFF2A303C),
        onSurfaceVariant = Color(0xFFC3C8D4),
        surfaceContainerLowest = Color(0xFF0C1018),
        surfaceContainerLow = Color(0xFF171C26),
        surfaceContainer = Color(0xFF1B212C),
        surfaceContainerHigh = Color(0xFF202634),
        surfaceContainerHighest = Color(0xFF262D3C),
        outline = Color(0xFF8D94A2),
        outlineVariant = Color(0xFF3A414F),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color(0xFF000000),
    )

@Immutable
data class PosStatusColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val LightStatusColors =
    PosStatusColors(
        success = Color(0xFF18865B),
        onSuccess = Color(0xFFFFFFFF),
        successContainer = Color(0xFFD6F2E4),
        onSuccessContainer = Color(0xFF04412A),
        warning = Color(0xFFB76A00),
        onWarning = Color(0xFFFFFFFF),
        warningContainer = Color(0xFFFFE9CE),
        onWarningContainer = Color(0xFF472900),
    )

internal val DarkStatusColors =
    PosStatusColors(
        success = Color(0xFF6FDCA9),
        onSuccess = Color(0xFF003822),
        successContainer = Color(0xFF0B5639),
        onSuccessContainer = Color(0xFFC6F3DE),
        warning = Color(0xFFFFB95C),
        onWarning = Color(0xFF452B00),
        warningContainer = Color(0xFF6E4300),
        onWarningContainer = Color(0xFFFFE1BD),
    )

internal val LocalPosStatusColors = staticCompositionLocalOf { LightStatusColors }

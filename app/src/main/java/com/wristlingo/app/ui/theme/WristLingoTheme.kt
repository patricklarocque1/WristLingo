package com.wristlingo.app.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Primary = Color(0xFF4F46E5)
private val Secondary = Color(0xFF22C55E)
private val Tertiary = Color(0xFFF97316)

private val Neutral10 = Color(0xFF111827)
private val Neutral20 = Color(0xFF1F2937)
private val Neutral90 = Color(0xFFE5E7EB)
private val Neutral95 = Color(0xFFF3F4F6)
private val Neutral99 = Color(0xFFFAFAFA)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Primary.copy(alpha = 0.12f),
    onPrimaryContainer = Primary,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Secondary.copy(alpha = 0.12f),
    onSecondaryContainer = Secondary,
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = Tertiary.copy(alpha = 0.12f),
    onTertiaryContainer = Tertiary,
    background = Neutral99,
    onBackground = Neutral20,
    surface = Neutral99,
    onSurface = Neutral20,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral20,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Primary.copy(alpha = 0.24f),
    onPrimaryContainer = Primary,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Secondary.copy(alpha = 0.24f),
    onSecondaryContainer = Secondary,
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = Tertiary.copy(alpha = 0.24f),
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral90,
)

@Composable
fun WristLingoTheme(
    useDarkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}



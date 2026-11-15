package com.example.soulspeak.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom Color Holder to keep the code compatible
data class SoulspeakColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val card: Color,
    val nav: Color,
    val text: Color,
    val textSecondary: Color,
    val softWhite: Color = Color(0xFFF0F2F5)
)

val MidnightAurora = SoulspeakColors(
    primary = PrimaryPurple,
    secondary = SoftPink,
    background = PrimaryDark,
    card = CardDark,
    nav = NavDark,
    text = SoftWhite,
    textSecondary = PrimaryPurple
)

val BlossomPink = SoulspeakColors(
    primary = GirlsPrimary,
    secondary = GirlsSecondary,
    background = GirlsBackground,
    card = GirlsCard,
    nav = GirlsNav,
    text = GirlsText,
    textSecondary = GirlsTextSecondary
)

val LocalSoulspeakColors = staticCompositionLocalOf { MidnightAurora }

object SoulspeakTheme {
    val colors: SoulspeakColors
        @Composable
        get() = LocalSoulspeakColors.current
}

@Composable
fun SoulspeakTheme(
    isGirlsTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val soulspeakColors = if (isGirlsTheme) BlossomPink else MidnightAurora
    
    val colorScheme = if (isGirlsTheme) {
        lightColorScheme(
            primary = soulspeakColors.primary,
            secondary = soulspeakColors.secondary,
            background = soulspeakColors.background,
            surface = soulspeakColors.card,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = soulspeakColors.text,
            onSurface = soulspeakColors.text
        )
    } else {
        darkColorScheme(
            primary = soulspeakColors.primary,
            secondary = soulspeakColors.secondary,
            background = soulspeakColors.background,
            surface = soulspeakColors.card,
            onPrimary = soulspeakColors.softWhite,
            onSecondary = soulspeakColors.softWhite,
            onBackground = soulspeakColors.softWhite,
            onSurface = soulspeakColors.softWhite
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = soulspeakColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isGirlsTheme
        }
    }

    CompositionLocalProvider(LocalSoulspeakColors provides soulspeakColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

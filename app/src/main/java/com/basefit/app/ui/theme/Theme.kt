package com.basefit.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

val Primary = Color(0xFF2563EB)
val PrimaryDark = Color(0xFF1D4ED8)
val Accent = Color(0xFF10B981)
val Background = Color(0xFFF8FAFC)
val Surface = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)
val TextHint = Color(0xFF94A3B8)
val Divider = Color(0xFFE2E8F0)
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)

val BodyweightColor = Color(0xFF3B82F6)
val StrengthColor = Color(0xFF8B5CF6)
val CardioColor = Color(0xFFF97316)

val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextHint = Color(0xFF64748B)
val DarkDivider = Color(0xFF334155)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = PrimaryDark,
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = PrimaryDark,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    outlineVariant = Color(0xFFE2E8F0),
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF60A5FA),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider,
    outlineVariant = Color(0xFF475569),
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun BaseFitTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDarkTheme
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val backgroundColor = if (darkTheme) DarkBackground else Background
    val surfaceColor = if (darkTheme) DarkSurface else Surface

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = backgroundColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
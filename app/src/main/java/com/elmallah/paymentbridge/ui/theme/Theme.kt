package com.elmallah.paymentbridge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.elmallah.paymentbridge.security.DeviceKeyManager

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = NavyDark,
    secondary = NavySecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE3EA),
    onSecondaryContainer = Color(0xFF191C1E),
    tertiary = BankAhlyTeal,
    onTertiary = Color.White,
    background = NeutralSurface,
    onBackground = TextPrimary,
    surface = NeutralCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    outline = NeutralBorder,
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = NavyDark,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFF94A3B8),
    onSecondary = NavyDark,
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFF4FD8CA),
    onTertiary = Color(0xFF003732),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun AlMallahTheme(
    themePreference: String = DeviceKeyManager.THEME_SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        DeviceKeyManager.THEME_LIGHT -> false
        DeviceKeyManager.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

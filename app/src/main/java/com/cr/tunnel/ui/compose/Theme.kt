package com.cr.tunnel.ui.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.cr.tunnel.AppConfig
import com.cr.tunnel.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val LightColor = lightColorScheme(
    primary = Color(0xFF00A8C4), // Neon Cyan
    onPrimary = Color(0xFFFFFFFF), // White
    primaryContainer = Color(0xFFB3F5FF), // Light Cyan
    onPrimaryContainer = Color(0xFF00262C), // Dark Cyan
    secondary = Color(0xFF7C4DFF), // Purple
    onSecondary = Color(0xFFFFFFFF), // White
    secondaryContainer = Color(0xFFEDE1FF), // Light Purple
    onSecondaryContainer = Color(0xFF1E0055), // Dark Purple
    tertiary = Color(0xFF00B86B), // Green
    onTertiary = Color(0xFFFFFFFF), // White
    tertiaryContainer = Color(0xFFA0F2D0), // Light Green
    onTertiaryContainer = Color(0xFF00201A), // Dark Teal
    error = Color(0xFFBA1A1A), // Red
    errorContainer = Color(0xFFFFDAD6), // Light Red
    onError = Color(0xFFFFFFFF), // White
    onErrorContainer = Color(0xFF410002), // Dark Red
    background = Color(0xFFF5FAFC), // Off White
    onBackground = Color(0xFF0A0F1E), // Dark Navy
    surface = Color(0xFFFFFFFF), // White
    onSurface = Color(0xFF0A0F1E), // Dark Navy
    surfaceVariant = Color(0xFFE4ECF2), // Light Gray Blue
    onSurfaceVariant = Color(0xFF3B4650), // Dark Gray
    outline = Color(0xFF00E5FF), // Cyan outline
    outlineVariant = Color(0xFFC8D6E0), // Light Gray
    inverseSurface = Color(0xFF1C2A3A), // Dark Navy
    inverseOnSurface = Color(0xFFF0F8FC), // Very Light Cyan
    inversePrimary = Color(0xFF00E5FF), // Neon Cyan
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF00A8C4), // Cyan
    surfaceContainerLowest = Color(0xFFFFFFFF), // White
    surfaceContainerLow = Color(0xFFF5FAFC), // Off White
    surfaceContainer = Color(0xFFEFF6FA), // Light Blue Gray
    surfaceContainerHigh = Color(0xFFE9F1F6), // Light Blue Gray
    surfaceContainerHighest = Color(0xFFE2EBF2), // Light Blue Gray
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFF00E5FF), // Neon Cyan
    onPrimary = Color(0xFF00262C), // Dark Cyan
    primaryContainer = Color(0xFF0B3A45), // Deep Cyan
    onPrimaryContainer = Color(0xFFB3F5FF), // Light Cyan
    secondary = Color(0xFFA855F7), // Purple
    onSecondary = Color(0xFF1E0055), // Dark Purple
    secondaryContainer = Color(0xFF3D1B66), // Deep Purple
    onSecondaryContainer = Color(0xFFEDE1FF), // Light Purple
    tertiary = Color(0xFF00D68F), // Mint Green
    onTertiary = Color(0xFF00382E), // Dark Teal
    tertiaryContainer = Color(0xFF005143), // Teal
    onTertiaryContainer = Color(0xFFA0F2D0), // Light Green
    error = Color(0xFFFFB4AB), // Light Red
    errorContainer = Color(0xFF93000A), // Dark Red
    onError = Color(0xFF690005), // Deep Red
    onErrorContainer = Color(0xFFFFDAD6), // Light Red
    background = Color(0xFF0A0E27), // Deep Midnight Blue (OLED)
    onBackground = Color(0xFFE8EFFF), // Light Cyan Blue
    surface = Color(0xFF0F1530), // Midnight Blue Surface
    onSurface = Color(0xFFE8EFFF), // Light Cyan Blue
    surfaceVariant = Color(0xFF1C2445), // Dark Blue Gray
    onSurfaceVariant = Color(0xFFA8B8D0), // Light Gray Blue
    outline = Color(0xFF00E5FF), // Neon Cyan
    outlineVariant = Color(0xFF26305A), // Dark Blue
    inverseSurface = Color(0xFFE0F0FF), // Light Cyan Blue
    inverseOnSurface = Color(0xFF1C2A3A), // Dark Navy
    inversePrimary = Color(0xFF00A8C4), // Cyan
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF00E5FF), // Neon Cyan
    surfaceContainerLowest = Color(0xFF070A1B), // Black Blue
    surfaceContainerLow = Color(0xFF0F1530), // Midnight Blue
    surfaceContainer = Color(0xFF161D3D), // Deep Midnight
    surfaceContainerHigh = Color(0xFF1D2547), // Lighter Midnight
    surfaceContainerHighest = Color(0xFF252E55), // Blue Gray
)

// Semantic Colors
val colorPing = Color(0xFF00D68F) // Mint Green
val colorPingRed = Color(0xFFFF2D78) // Hot Pink Red
val colorConfigType = Color(0xFF00E5FF) // Neon Cyan
val colorFabActive = Color(0xFFA855F7) // Purple
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF233052) // Dark Blue

// Toast Colors 70%
val toastNormalBgLight = Color(0xB3353A3E) // Dark Gray
val toastNormalBgDark = Color(0xB34A4F54) // Darker Gray
val toastSuccessBg = Color(0xB300D68F) // Mint Green
val toastErrorBg = Color(0xB3D50000) // Red
val toastInfoBg = Color(0xB300E5FF) // Neon Cyan
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColor else LightColor
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                GlassBackground(darkTheme = darkTheme) {
                    AppSnackbarBridge(controller = snackbarController)
                    content()
                    AppSnackbarHost(hostState = snackbarController.hostState)
                }
            }
        }
    }
}

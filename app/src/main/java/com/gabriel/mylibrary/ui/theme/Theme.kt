package com.gabriel.mylibrary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6E9E), onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EEF8), onPrimaryContainer = Color(0xFF245A83),
    secondary = Color(0xFF47657C), secondaryContainer = Color(0xFFE7ECF4),
    onSecondaryContainer = Color(0xFF233F55),
    background = Color(0xFFF5F7FB), onBackground = Color(0xFF14212D),
    surface = Color.White, onSurface = Color(0xFF14212D),
    surfaceVariant = Color(0xFFE7ECF4), onSurfaceVariant = Color(0xFF566575),
    surfaceContainer = Color.White, surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFEDF2F8),
    outline = Color(0xFF76899B), outlineVariant = Color(0xFFD8E0ED)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BCCF2), onPrimary = Color(0xFF103951),
    primaryContainer = Color(0xFF234760), onPrimaryContainer = Color(0xFFD4EAFF),
    secondary = Color(0xFFB4CADB), secondaryContainer = Color(0xFF2B3F50),
    onSecondaryContainer = Color(0xFFD9EAF5),
    background = Color(0xFF101820), onBackground = Color(0xFFE4EDF5),
    surface = Color(0xFF1B2631), onSurface = Color(0xFFE4EDF5),
    surfaceVariant = Color(0xFF2B3B4B), onSurfaceVariant = Color(0xFFB6C5D3),
    surfaceContainer = Color(0xFF1B2631), surfaceContainerLow = Color(0xFF1B2631),
    surfaceContainerHigh = Color(0xFF263543),
    outline = Color(0xFF91A4B7), outlineVariant = Color(0xFF384C60)
)

@Composable
fun MyLibraryTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = base.copy(headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold)),
        content = content
    )
}

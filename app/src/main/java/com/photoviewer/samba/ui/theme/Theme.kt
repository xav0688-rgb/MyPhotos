package com.photoviewer.samba.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary        = Color(0xFFBB86FC),
    secondary      = Color(0xFF03DAC6),
    background     = Color(0xFF121212),
    surface        = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    onPrimary      = Color.Black,
    onSecondary    = Color.Black,
    onBackground   = Color.White,
    onSurface      = Color(0xFFE0E0E0),
    error          = Color(0xFFCF6679)
)

@Composable
fun SambaPhotoViewerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}

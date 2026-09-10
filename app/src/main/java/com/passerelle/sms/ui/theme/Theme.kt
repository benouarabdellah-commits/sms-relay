package com.passerelle.sms.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = Pine,
    onPrimary = Color.White,
    secondary = Teal,
    background = Mist,
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    outline = Line,
    error = Danger
)

@Composable
fun PasserelleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        content = content
    )
}

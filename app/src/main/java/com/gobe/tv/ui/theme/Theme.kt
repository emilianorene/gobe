package com.gobe.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@Composable
fun GobeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = GobeAccent,
            background = GobeBg,
            surface = GobeSurface,
            onBackground = GobeOnDark,
            onSurface = GobeOnDark,
        ),
        typography = GobeTypography,
        content = content,
    )
}

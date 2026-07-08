package com.gobe.tv.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Console controller art floating on the dark stage, lit by an accent radial glow behind it —
 * matching Gobe's established tile style (dark surface + accent glow, brighter on focus). The art
 * assets are transparent PNGs (photos) or transparent vectors (Arcade), so there is no plate and
 * no border: the controller reads directly against the tinted stage.
 */
@Composable
fun ConsoleHero(art: ConsoleArt, focused: Boolean, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        art.accent.copy(alpha = if (focused) 0.45f else 0.20f),
                        Color.Transparent,
                    ),
                ),
            ),
        )
        Image(
            painter = painterResource(art.art),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(0.82f).padding(8.dp),
        )
    }
}

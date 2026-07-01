package com.gobe.tv.emulation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.R

/** Brief, non-intrusive pill shown at the bottom when a game starts, teaching how to open the menu. */
@Composable
fun ControlsHint(comboLabel: String) {
    Box(Modifier.fillMaxSize().padding(bottom = 40.dp), contentAlignment = Alignment.BottomCenter) {
        Text(
            stringResource(R.string.controls_hint_menu_fmt, comboLabel),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

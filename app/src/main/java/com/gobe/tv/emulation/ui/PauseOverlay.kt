package com.gobe.tv.emulation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * Pause overlay drawn on top of the running game. D-pad navigable; "Reanudar" gets initial focus.
 * "Cargar estado" is disabled when there is no saved state.
 */
@Composable
fun PauseOverlay(
    hasState: Boolean,
    onResume: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onExit: () -> Unit,
) {
    val resumeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { resumeFocus.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(64.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("⏸  Pausa", style = MaterialTheme.typography.headlineMedium, color = Color.White)

        Button(
            onClick = onResume,
            modifier = Modifier.width(320.dp).focusRequester(resumeFocus),
        ) { Text("▶  Reanudar") }

        Button(onClick = onSave, modifier = Modifier.width(320.dp)) { Text("💾  Guardar estado") }

        Button(onClick = onLoad, enabled = hasState, modifier = Modifier.width(320.dp)) {
            Text(if (hasState) "📂  Cargar estado" else "📂  Cargar estado (sin guardado)")
        }

        Button(onClick = onExit, modifier = Modifier.width(320.dp)) { Text("✕  Salir al menú") }
    }
}

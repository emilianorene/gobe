package com.gobe.tv.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import com.gobe.tv.domain.Game

@Composable
fun DetailScreen(app: GobeApp, gameId: Long, onBack: () -> Unit) {
    BackHandler { onBack() }
    var game by remember { mutableStateOf<Game?>(null) }
    LaunchedEffect(gameId) { game = app.repository.getGame(gameId) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(game) { if (game != null) runCatching { focus.requestFocus() } }

    val g = game
    Column(Modifier.fillMaxSize().padding(64.dp)) {
        if (g == null) {
            Text("Cargando…")
        } else {
            Text(g.displayName, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "${g.system.displayName} · ${g.sizeBytes / 1024} KB",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = { }, enabled = false) { Text("▶ Jugar (proximamente)") }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack, modifier = Modifier.focusRequester(focus)) { Text("Volver") }
        }
    }
}

package com.gobe.tv.ui.detail

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.System
import com.gobe.tv.emulation.EmulatorActivity
import com.gobe.tv.emulation.EmulatorArgs
import com.gobe.tv.emulation.SaveStateStore
import com.gobe.tv.emulation.putEmulatorArgs

@Composable
fun DetailScreen(app: GobeApp, gameId: Long, onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var game by remember { mutableStateOf<Game?>(null) }
    LaunchedEffect(gameId) { game = app.repository.getGame(gameId) }
    val playFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }

    val g = game
    val playable = g?.system == System.SNES
    var hasState by remember { mutableStateOf(false) }

    // Refresh "save state exists" on load AND whenever we return from the emulator (ON_RESUME),
    // so the resume-from-save button appears right after saving/playing without leaving the screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(g) { if (g != null) hasState = SaveStateStore(context.filesDir).hasState(g.id) }
    DisposableEffect(lifecycleOwner, g) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && g != null) {
                hasState = SaveStateStore(context.filesDir).hasState(g.id)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(g, playable) {
        if (g != null) runCatching { (if (playable) playFocus else backFocus).requestFocus() }
    }

    fun launch(loadState: Boolean) {
        val current = g ?: return
        val intent = Intent(context, EmulatorActivity::class.java)
            .putEmulatorArgs(
                EmulatorArgs(
                    gameId = current.id,
                    romPath = current.path,
                    system = current.system,
                    loadState = loadState,
                ),
            )
        context.startActivity(intent)
    }

    Column(Modifier.fillMaxSize().padding(64.dp)) {
        if (g == null) {
            Text(stringResource(R.string.detail_loading))
        } else {
            Text(g.displayName, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "${g.system.displayName} · " + stringResource(R.string.detail_size_kb, (g.sizeBytes / 1024).toInt()) +
                    if (hasState) " · " + stringResource(R.string.detail_save_state_present) else "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(32.dp))
            if (playable) {
                Button(
                    onClick = { launch(loadState = false) },
                    modifier = Modifier.focusRequester(playFocus),
                ) { Text("▶ " + stringResource(R.string.detail_play)) }
                if (hasState) {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { launch(loadState = true) }) { Text("⟳ " + stringResource(R.string.detail_resume_save)) }
                }
            } else {
                Button(onClick = { }, enabled = false) { Text("▶ " + stringResource(R.string.detail_play_soon)) }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack, modifier = Modifier.focusRequester(backFocus)) { Text(stringResource(R.string.detail_back)) }
        }
    }
}

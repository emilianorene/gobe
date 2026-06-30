package com.gobe.tv.ui.detail

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.data.art.BoxartUrlBuilder
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
        (context.applicationContext as GobeApp).returnToHomeOnResume = true
        context.startActivity(intent)
    }

    Box(Modifier.fillMaxSize().padding(64.dp)) {
        if (g == null) {
            Text(stringResource(R.string.detail_loading))
        } else {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                // LEFT: large cover art
                CoverArt(g)

                Spacer(Modifier.width(48.dp))

                // RIGHT: metadata + actions
                Column(Modifier.weight(1f)) {
                    Text(g.displayName, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        g.system.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.detail_players) + ": " + (g.players?.toString() ?: "—"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.detail_size_kb, (g.sizeBytes / 1024).toInt()) +
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
    }
}

@Composable
private fun CoverArt(game: Game) {
    val shape = RoundedCornerShape(12.dp)
    val url = BoxartUrlBuilder().url(game.system, game.boxartName ?: game.displayName)
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(300.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (url != null) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = game.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                }
                // loading / error / null-URL -> placeholder surface (the Box background)
            }
        }
    }
}

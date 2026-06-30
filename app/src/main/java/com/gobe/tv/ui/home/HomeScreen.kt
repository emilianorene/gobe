package com.gobe.tv.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.ui.folders.vmFactory

@Composable
fun HomeScreen(app: GobeApp, onOpenGame: (Long) -> Unit, onOpenFolders: () -> Unit) {
    val vm: HomeViewModel = viewModel(
        factory = vmFactory { HomeViewModel(app.repository, app.defaultRomPath) }
    )
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.gobe_logo),
                contentDescription = "Gobe",
                modifier = Modifier.height(56.dp),
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = onOpenFolders) { Text("⚙ " + stringResource(R.string.home_settings)) }
        }
        Spacer(Modifier.height(24.dp))

        when {
            state.loading -> Text(stringResource(R.string.home_scanning), style = MaterialTheme.typography.bodyLarge)
            state.rows.isEmpty() -> EmptyState(onOpenFolders)
            else -> {
                val hasContinue = state.continuePlaying.isNotEmpty()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    if (hasContinue) {
                        item {
                            Column {
                                Text(stringResource(R.string.home_continue_playing), style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(state.continuePlaying) { g ->
                                        val attachFocus = g == state.continuePlaying.first()
                                        GameTile(
                                            game = g,
                                            onClick = { onOpenGame(g.id) },
                                            requestInitialFocus = attachFocus,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items(state.rows) { row ->
                        val system = row.first
                        val games = row.second
                        Column {
                            Text(system.displayName, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            // When the continue-playing row is present it owns initial focus,
                            // so the first system row must not also request it.
                            val isFirstRow = !hasContinue && system == state.rows.first().first
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(games) { g ->
                                    val attachFocus = isFirstRow && g == games.first()
                                    GameTile(
                                        game = g,
                                        onClick = { onOpenGame(g.id) },
                                        requestInitialFocus = attachFocus,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onOpenFolders: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.home_no_games), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.home_add_folder_hint), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onOpenFolders, modifier = Modifier.focusRequester(focus)) {
            Text(stringResource(R.string.home_configure_folders))
        }
    }
}

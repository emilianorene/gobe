package com.gobe.tv.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.domain.SortMode
import com.gobe.tv.ui.folders.vmFactory
import com.gobe.tv.ui.home.GameTile

@Composable
fun LibraryScreen(app: GobeApp, section: LibrarySection, onOpenGame: (Long) -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    // MUST key by section: there is one ViewModelStoreOwner (the Activity), so an unkeyed
    // viewModel() caches by class name and a second section would reuse the first's VM (showing the
    // wrong games). `section.toString()` is a stable per-section key (data class/object toString).
    val vm: LibraryViewModel = viewModel(key = section.toString(), factory = vmFactory { LibraryViewModel(app.repository, section) })
    val games by vm.games.collectAsState()
    val genres by vm.genres.collectAsState()
    val selectedGenre by vm.selectedGenre.collectAsState()
    val sortMode by vm.sortMode.collectAsState()

    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Text(sectionTitle(section), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Sort control (cycles Recommended -> Title -> Year).
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.cycleSort() }) {
                Text("↕ " + stringResource(R.string.sort_label) + ": " + sortLabel(sortMode))
            }
        }

        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Button(onClick = { vm.setGenre(null) }) {
                        Text((if (selectedGenre == null) "● " else "") + stringResource(R.string.filter_all))
                    }
                }
                items(genres, key = { it }) { genre ->
                    Button(onClick = { vm.setGenre(if (selectedGenre == genre) null else genre) }) {
                        Text((if (selectedGenre == genre) "● " else "") + genre)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        if (games.isEmpty()) {
            Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(132.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(games, key = { it.id }) { g ->
                    GameTile(game = g, onClick = { onOpenGame(g.id) }, requestInitialFocus = g == games.first())
                }
            }
        }
    }
}

@Composable
private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.RECOMMENDED -> stringResource(R.string.sort_recommended)
    SortMode.TITLE -> stringResource(R.string.sort_title)
    SortMode.YEAR -> stringResource(R.string.sort_year)
}

@Composable
private fun sectionTitle(section: LibrarySection): String = when (section) {
    is LibrarySection.Console -> section.system.displayName
    LibrarySection.Recommended -> stringResource(R.string.section_recommended)
    LibrarySection.Favorites -> stringResource(R.string.section_favorites)
    is LibrarySection.SearchAll -> "\"" + section.query + "\""
}

package com.gobe.tv.ui.library

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.SortMode
import com.gobe.tv.emulation.CoreManager
import com.gobe.tv.emulation.EmulatorActivity
import com.gobe.tv.emulation.EmulatorArgs
import com.gobe.tv.emulation.SaveStateStore
import com.gobe.tv.emulation.putEmulatorArgs
import com.gobe.tv.ui.detail.GameDetailPanel
import com.gobe.tv.ui.folders.vmFactory
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(app: GobeApp, section: LibrarySection, onBack: () -> Unit) {
    BackHandler { onBack() }
    // MUST key by section: there is one ViewModelStoreOwner (the Activity), so an unkeyed
    // viewModel() caches by class name and a second section would reuse the first's VM (showing the
    // wrong games). `section.toString()` is a stable per-section key (data class/object toString).
    val vm: LibraryViewModel = viewModel(key = section.toString(), factory = vmFactory { LibraryViewModel(app.repository, section) })
    val games by vm.games.collectAsState()
    val genres by vm.genres.collectAsState()
    val selectedGenre by vm.selectedGenre.collectAsState()
    val sortMode by vm.sortMode.collectAsState()
    val collectionFilter by vm.collectionFilter.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Which row is currently focused (drives the detail panel). Falls back to the first game.
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val focused: Game? = games.firstOrNull { it.id == focusedId } ?: games.firstOrNull()
    // Keep focusedId valid when the list changes (genre/sort filter, refresh).
    LaunchedEffect(games) {
        if (games.none { it.id == focusedId }) focusedId = games.firstOrNull()?.id
    }

    // Detail-panel side state, mirroring DetailScreen but tracking the focused game.
    val playable = focused != null &&
        CoreManager(context.applicationInfo.nativeLibraryDir).corePath(focused.system) != null

    var hasState by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(focused) {
        hasState = if (focused != null) SaveStateStore(context.filesDir).hasState(focused.id) else false
    }
    DisposableEffect(lifecycleOwner, focused) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && focused != null) {
                hasState = SaveStateStore(context.filesDir).hasState(focused.id)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var favorite by remember { mutableStateOf(false) }
    LaunchedEffect(focused) { favorite = focused?.favorite ?: false }

    val playFocus = remember { FocusRequester() }
    val railFocus = remember { FocusRequester() }

    // Land focus on the rail's focused row (row 0 initially). Key on `focusedId == null` so this
    // re-fires after LaunchedEffect(games) populates focusedId — at which point railFocus is
    // attached to that row. Guarded so it never requests against an empty list.
    LaunchedEffect(focusedId == null) {
        if (focused != null) runCatching { railFocus.requestFocus() }
    }

    fun launch(loadState: Boolean) {
        val current = focused ?: return
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

    Column(
        Modifier.fillMaxSize().padding(40.dp).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val action = keyToLibraryAction(event.key.nativeKeyCode) ?: return@onPreviewKeyEvent false
            val next = action == LibraryKeyAction.PageNext
            scope.launch {
                val n = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                val target = (listState.firstVisibleItemIndex + (if (next) n else -n))
                    .coerceIn(0, (games.size - 1).coerceAtLeast(0))
                // Scroll first so the target row is composed, then move focusedId (which moves the
                // railFocus requester onto that row) and request focus — panel follows via onFocused.
                listState.scrollToItem(target)
                games.getOrNull(target)?.let { focusedId = it.id }
                runCatching { railFocus.requestFocus() }
            }
            true
        },
    ) {
        // Top bar: title + count, sort button, genre chips.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                sectionTitle(section) + " · ${games.size}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.weight(1f))
            FilterChip(stringResource(R.string.filter_all), collectionFilter == CollectionFilter.ALL) { vm.setFilter(CollectionFilter.ALL) }
            Spacer(Modifier.width(8.dp))
            FilterChip("★ " + stringResource(R.string.section_recommended), collectionFilter == CollectionFilter.RECOMMENDED) {
                vm.setFilter(if (collectionFilter == CollectionFilter.RECOMMENDED) CollectionFilter.ALL else CollectionFilter.RECOMMENDED)
            }
            Spacer(Modifier.width(8.dp))
            FilterChip("♥ " + stringResource(R.string.section_favorites), collectionFilter == CollectionFilter.FAVORITES) {
                vm.setFilter(if (collectionFilter == CollectionFilter.FAVORITES) CollectionFilter.ALL else CollectionFilter.FAVORITES)
            }
            Spacer(Modifier.width(12.dp))
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

        if (games.isEmpty() || focused == null) {
            // Single centered empty-state message; skip the two-pane layout entirely.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                // LEFT rail: poster rows. The railFocus requester is attached to whichever row is
                // the focused one, so page-jump can move focus to the target.
                LazyColumn(
                    // RIGHT out of the rail is redirected to the panel's Play button (which is now
                    // always visible thanks to the anchored-actions layout). Down/Up within the panel
                    // then navigate Resume/Favorite normally.
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .focusProperties { right = playFocus },
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(games, key = { _, g -> g.id }) { _, g ->
                        GameRow(
                            game = g,
                            onClick = { focusedId = g.id; launch(false) },
                            onFocused = { focusedId = g.id },
                            focusRequester = if (g.id == focusedId) railFocus else null,
                        )
                    }
                }

                Spacer(Modifier.width(24.dp))

                // RIGHT panel: live detail for the focused game.
                Box(Modifier.weight(0.6f).fillMaxHeight()) {
                    GameDetailPanel(
                        game = focused,
                        playable = playable,
                        hasState = hasState,
                        favorite = favorite,
                        onPlay = { launch(false) },
                        onResume = { launch(true) },
                        onToggleFavorite = {
                            val id = focused.id
                            favorite = !favorite
                            scope.launch { app.repository.updateFavorite(id, favorite) }
                        },
                        playFocusRequester = playFocus,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        LibraryControlLegend()
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick) { Text((if (selected) "● " else "") + label) }
}

@Composable
private fun LibraryControlLegend() {
    val play = stringResource(R.string.legend_play)
    val back = stringResource(R.string.legend_back)
    val actions = stringResource(R.string.legend_actions)
    val page = stringResource(R.string.legend_page)
    Text(
        "Ⓐ $play  ·  Ⓑ $back  ·  ▶ $actions  ·  L1/R1 $page",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
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

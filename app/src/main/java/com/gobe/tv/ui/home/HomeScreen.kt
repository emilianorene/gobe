package com.gobe.tv.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.folders.vmFactory

@Composable
fun HomeScreen(app: GobeApp, onOpenGame: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val vm: HomeViewModel = viewModel(
        factory = vmFactory { HomeViewModel(app.repository, app.defaultRomPath) }
    )
    val state by vm.state.collectAsState()
    val query by vm.query.collectAsState()
    val selectedSystem by vm.selectedSystem.collectAsState()
    val selectedGenre by vm.selectedGenre.collectAsState()
    val genres by vm.genres.collectAsState()
    val settingsFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(40.dp)
            .onPreviewKeyEvent { event ->
                val action = keyToHomeAction(event.key.nativeKeyCode) ?: return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) {
                    when (action) {
                        HomeKeyAction.Search -> { runCatching { searchFocus.requestFocus() }; keyboard?.show() }
                        HomeKeyAction.Settings -> onOpenSettings()
                    }
                }
                true // consume KeyDown (act) and KeyUp (no-op) for L1/R1
            },
    ) {
        // Top bar: logo + search field + Settings.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.gobe_logo),
                contentDescription = "Gobe",
                modifier = Modifier.height(56.dp),
            )
            Spacer(Modifier.width(24.dp))
            SearchField(
                value = query,
                onValueChange = vm::setQuery,
                focusRequester = searchFocus,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(24.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.focusRequester(settingsFocus),
            ) { Text("⚙ " + stringResource(R.string.home_settings)) }
        }
        Spacer(Modifier.height(16.dp))

        // Filter chips: All + one per system.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                label = stringResource(R.string.filter_all),
                selected = selectedSystem == null,
                onClick = { vm.setSystem(null) },
            )
            System.entries.forEach { sys ->
                FilterChip(
                    label = sys.displayName,
                    selected = selectedSystem == sys,
                    onClick = { vm.setSystem(sys) },
                )
            }
        }

        // Genre chips: a scrollable second row (genres are unbounded, unlike the fixed system enum).
        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    FilterChip(
                        label = stringResource(R.string.filter_all),
                        selected = selectedGenre == null,
                        onClick = { vm.setGenre(null) },
                    )
                }
                items(genres, key = { it }) { genre ->
                    FilterChip(
                        label = genre,
                        selected = selectedGenre == genre,
                        // Re-tapping the selected genre clears back to All.
                        onClick = { vm.setGenre(if (selectedGenre == genre) null else genre) },
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        val hasContinue = state.continuePlaying.isNotEmpty()

        // Content fills the remaining height (weighted) so the control legend stays pinned below it.
        Box(Modifier.weight(1f).fillMaxWidth()) {
        when {
            state.loading -> Text(stringResource(R.string.home_scanning), style = MaterialTheme.typography.bodyLarge)
            state.games.isEmpty() && !hasContinue && query.isEmpty() && selectedSystem == null ->
                EmptyState(onOpenSettings)
            else -> {
                // Single scroll: "Continue playing" is a full-span header inside the grid, so it
                // scrolls away instead of pinning above a separate grid.
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(132.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (hasContinue) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            // Topmost focusable content row routes UP to the top-bar Settings button.
                            Column(Modifier.focusProperties { up = settingsFocus }) {
                                Text(
                                    stringResource(R.string.home_continue_playing),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(state.continuePlaying, key = { it.id }) { g ->
                                        GameTile(
                                            game = g,
                                            onClick = { onOpenGame(g.id) },
                                            requestInitialFocus = g == state.continuePlaying.first(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items(state.games, key = { it.id }) { g ->
                        GameTile(
                            game = g,
                            onClick = { onOpenGame(g.id) },
                            requestInitialFocus = !hasContinue && g == state.games.first(),
                            // With no continue row, the first grid tile routes UP to Settings.
                            modifier = if (!hasContinue && g == state.games.first())
                                Modifier.focusProperties { up = settingsFocus } else Modifier,
                        )
                    }
                }
            }
        }
        }
        HomeControlLegend()
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = textColor, fontSize = 16.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(textColor),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).focusRequester(focusRequester),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.5f),
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(if (selected) "● $label" else label)
    }
}

/** Subtle, always-visible control legend pinned at the bottom of Home. Labels match the real
 *  bindings (A = select, B = back) plus the L1/R1 shortcuts. */
@Composable
private fun HomeControlLegend() {
    val select = stringResource(R.string.legend_select)
    val back = stringResource(R.string.legend_back)
    val search = stringResource(R.string.legend_search)
    val settings = stringResource(R.string.legend_settings)
    Text(
        "Ⓐ $select  ·  Ⓑ $back  ·  L1 $search  ·  R1 $settings",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}

@Composable
private fun EmptyState(onOpenSettings: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.home_no_games), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.home_add_folder_hint), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onOpenSettings, modifier = Modifier.focusRequester(focus)) {
            Text(stringResource(R.string.home_configure_folders))
        }
    }
}

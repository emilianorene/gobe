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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.folders.vmFactory
import com.gobe.tv.ui.library.LibrarySection

@Composable
fun HomeScreen(
    app: GobeApp,
    onOpenSection: (LibrarySection) -> Unit,
    onOpenGame: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = vmFactory { HomeViewModel(app.repository, app.defaultRomPath) })
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val settingsFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Level-1 sections: special collections first, then one per console.
    val sections: List<Pair<String, LibrarySection>> = buildList {
        add(stringResource(R.string.section_recommended) to LibrarySection.Recommended)
        add(stringResource(R.string.section_favorites) to LibrarySection.Favorites)
        System.entries.forEach { add(it.displayName to LibrarySection.Console(it)) }
    }

    Column(
        Modifier.fillMaxSize().padding(40.dp).onPreviewKeyEvent { event ->
            val action = keyToHomeAction(event.key.nativeKeyCode) ?: return@onPreviewKeyEvent false
            if (event.type == KeyEventType.KeyDown) when (action) {
                HomeKeyAction.Search -> { runCatching { searchFocus.requestFocus() }; keyboard?.show() }
                HomeKeyAction.Settings -> onOpenSettings()
            }
            true
        },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.gobe_logo), "Gobe", modifier = Modifier.height(56.dp))
            Spacer(Modifier.width(24.dp))
            SearchField(
                value = query, onValueChange = { query = it },
                onSubmit = { if (query.isNotBlank()) onOpenSection(LibrarySection.SearchAll(query.trim())) },
                focusRequester = searchFocus, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(24.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.focusRequester(settingsFocus)) {
                Text("⚙ " + stringResource(R.string.home_settings))
            }
        }
        Spacer(Modifier.height(24.dp))

        val hasContinue = state.continuePlaying.isNotEmpty()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> Text(stringResource(R.string.home_scanning), style = MaterialTheme.typography.bodyLarge)
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (hasContinue) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                Text(stringResource(R.string.home_continue_playing), style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(state.continuePlaying, key = { it.id }) { g ->
                                        GameTile(game = g, onClick = { onOpenGame(g.id) },
                                            requestInitialFocus = g == state.continuePlaying.first())
                                    }
                                }
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(stringResource(R.string.home_consoles), style = MaterialTheme.typography.titleLarge)
                    }
                    itemsIndexed(sections) { i, (label, section) ->
                        SectionTile(
                            label = label,
                            section = section,
                            onClick = { onOpenSection(section) },
                            requestInitialFocus = !hasContinue && i == 0,
                        )
                    }
                }
            }
        }
        HomeControlLegend()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionTile(
    label: String,
    section: LibrarySection,
    onClick: () -> Unit,
    requestInitialFocus: Boolean,
) {
    val visual = sectionVisual(section)
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    if (requestInitialFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Card(
        onClick = onClick,
        modifier = (if (requestInitialFocus) Modifier.focusRequester(focus) else Modifier)
            .fillMaxWidth().aspectRatio(1.3f)
            .onFocusChanged { focused = it.isFocused },
        colors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(
                            visual.accent.copy(alpha = if (focused) 0.55f else 0.32f),
                            Color.Transparent,
                        ),
                        radius = 220f,
                    )
                )
            )
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(visual.iconRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.72f).weight(1f),
                )
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit,
    focusRequester: FocusRequester, modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun submit() { keyboard?.hide(); onSubmit(); focusManager.moveFocus(FocusDirection.Down) }
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = TextStyle(color = textColor, fontSize = 16.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(textColor),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { submit() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).focusRequester(focusRequester)
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) {
                        keyboard?.hide(); focusManager.moveFocus(FocusDirection.Down); true
                    } else false
                },
            decorationBox = { inner ->
                if (value.isEmpty()) Text(stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge, color = textColor.copy(alpha = 0.5f))
                inner()
            },
        )
    }
}

@Composable
private fun HomeControlLegend() {
    val select = stringResource(R.string.legend_select)
    val back = stringResource(R.string.legend_back)
    val search = stringResource(R.string.legend_search)
    val settings = stringResource(R.string.legend_settings)
    Text("Ⓐ $select  ·  Ⓑ $back  ·  L1 $search  ·  R1 $settings",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
}

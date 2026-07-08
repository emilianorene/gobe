package com.gobe.tv.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.graphics.lerp
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.System
import com.gobe.tv.ui.folders.vmFactory
import com.gobe.tv.ui.library.LibrarySection
import com.gobe.tv.ui.theme.GobeBg

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
    val searchFocus = remember { FocusRequester() }
    val heroFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val focused = state.focusedSystem
    val accent = if (focused != null) consoleArt(focused).accent else MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(
        lerp(GobeBg, accent, 0.22f), label = "stageBg",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bg, GobeBg)))
            .padding(40.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                keyToHomeAction(event.key.nativeKeyCode)?.let { action ->
                    when (action) {
                        HomeKeyAction.Search -> { runCatching { searchFocus.requestFocus() }; keyboard?.show() }
                        HomeKeyAction.Settings -> onOpenSettings()
                    }
                    return@onPreviewKeyEvent true
                }
                false
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
            Button(onClick = onOpenSettings) { Text("⚙ " + stringResource(R.string.home_settings)) }
        }
        Spacer(Modifier.height(24.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> Text(
                    stringResource(R.string.home_scanning),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
                focused == null -> EmptyLibrary(onOpenSettings = onOpenSettings, modifier = Modifier.align(Alignment.Center))
                else -> CarouselStage(
                    consoles = state.consoles,
                    focused = focused,
                    accent = accent,
                    continueForFocused = state.continueForFocused,
                    heroFocus = heroFocus,
                    onMove = { delta -> vm.move(delta) },
                    onOpenConsole = { onOpenSection(LibrarySection.Console(focused)) },
                    onOpenGame = onOpenGame,
                    onFocusUp = { runCatching { searchFocus.requestFocus() } },
                )
            }
        }
        HomeControlLegend()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CarouselStage(
    consoles: List<ConsoleEntry>,
    focused: System,
    accent: Color,
    continueForFocused: List<Game>,
    heroFocus: FocusRequester,
    onMove: (Int) -> Unit,
    onOpenConsole: () -> Unit,
    onOpenGame: (Long) -> Unit,
    onFocusUp: () -> Unit,
) {
    val count = consoles.firstOrNull { it.system == focused }?.count ?: 0
    val hasContinue = continueForFocused.isNotEmpty()
    val focusManager = LocalFocusManager.current
    val idx = consoles.indexOfFirst { it.system == focused }

    LaunchedEffect(Unit) { runCatching { heroFocus.requestFocus() } }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("‹", style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = if (idx <= 0) 0.08f else 0.25f),
                modifier = Modifier.align(Alignment.CenterStart))
            Text("›", style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = if (idx >= consoles.lastIndex) 0.08f else 0.25f),
                modifier = Modifier.align(Alignment.CenterEnd))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                var heroFocused by remember { mutableStateOf(false) }
                val heroScale by animateFloatAsState(if (heroFocused) 1.05f else 1f, label = "heroScale")
                Box(
                    modifier = Modifier
                        .focusRequester(heroFocus)
                        .fillMaxWidth(0.42f)
                        .aspectRatio(1.4f)
                        .scale(heroScale)
                        .onFocusChanged { heroFocused = it.isFocused }
                        .focusable()
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.key) {
                                Key.DirectionLeft -> { onMove(-1); true }
                                Key.DirectionRight -> { onMove(+1); true }
                                Key.DirectionUp -> { onFocusUp(); true }
                                Key.DirectionDown -> {
                                    if (hasContinue) { focusManager.moveFocus(FocusDirection.Down); true } else true
                                }
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { onOpenConsole(); true }
                                else -> false
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    ConsoleHero(consoleArt(focused), focused = heroFocused, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(20.dp))
                Text(focused.displayName, fontSize = 44.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("$count " + stringResource(R.string.home_games_count),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                Spacer(Modifier.height(16.dp))
                ConsoleDots(consoles = consoles, focused = focused, accent = accent)
            }
        }

        if (hasContinue) {
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_continue_playing),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(continueForFocused, key = { it.id }) { g ->
                        GameTile(game = g, onClick = { onOpenGame(g.id) }, requestInitialFocus = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleDots(consoles: List<ConsoleEntry>, focused: System, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        consoles.forEach { entry ->
            val on = entry.system == focused
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) accent else MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    entry.system.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.home_no_games), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings) { Text(stringResource(R.string.home_add_roms)) }
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

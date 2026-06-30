package com.gobe.tv.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.domain.Game

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GameTile(
    game: Game,
    onClick: () -> Unit,
    requestInitialFocus: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // When this is the designated first tile, own a FocusRequester and request focus once
    // it enters composition. Requesting from the tile itself (rather than the parent)
    // guarantees the requester is attached to a real node, avoiding a dead-remote no-op.
    val focusRequester = remember { FocusRequester() }
    if (requestInitialFocus) {
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    }
    val focusModifier = if (requestInitialFocus) Modifier.focusRequester(focusRequester) else Modifier

    Card(
        onClick = onClick,
        modifier = modifier.then(focusModifier).width(140.dp).height(180.dp),
        colors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                game.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

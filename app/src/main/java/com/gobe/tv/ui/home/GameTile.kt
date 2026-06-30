package com.gobe.tv.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.gobe.tv.data.art.BoxartUrlBuilder
import com.gobe.tv.domain.Game

private val boxartUrlBuilder = BoxartUrlBuilder()

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
    val url = boxartUrlBuilder.url(game.system, game.boxartName ?: game.displayName)

    Card(
        onClick = onClick,
        modifier = modifier.then(focusModifier).width(140.dp).height(190.dp),
        colors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (url != null) {
                SubcomposeAsyncImage(
                    model = url,
                    contentDescription = game.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (painter.state is AsyncImagePainter.State.Success) {
                        SubcomposeAsyncImageContent()
                    } else {
                        TextTile(game)
                    }
                }
            } else {
                TextTile(game)
            }

            val p = game.players
            if (p != null && p >= 2) {
                Text(
                    "👥$p",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TextTile(game: Game) {
    Box(Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            game.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

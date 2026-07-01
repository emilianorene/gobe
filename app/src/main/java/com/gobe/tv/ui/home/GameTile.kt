package com.gobe.tv.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.gobe.tv.R
import com.gobe.tv.data.art.BoxartUrlBuilder
import com.gobe.tv.domain.Game

private val boxartUrlBuilder = BoxartUrlBuilder()

/** Tile caption: game name, plus " (year)" when a real year is known. Pure for unit testing. */
fun tileCaption(name: String, year: Int?): String =
    if (year != null && year > 0) "$name ($year)" else name

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

    Column(
        modifier = modifier.then(focusModifier).width(132.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f), // square cover
            colors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (url != null) {
                    SubcomposeAsyncImage(
                        model = url,
                        contentDescription = game.displayName,
                        // Fit (not Crop) so the whole box art is shown without cutting; covers have
                        // varying aspect ratios, so some get letterboxed against the tile surface.
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (painter.state is AsyncImagePainter.State.Success) {
                            SubcomposeAsyncImageContent()
                        } else {
                            DefaultCover(game)
                        }
                    }
                } else {
                    DefaultCover(game)
                }

                val p = game.players
                if (p != null && p >= 2) {
                    Text(
                        "👥$p",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tileCaption(game.displayName, game.year),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
    }
}

/** Branded placeholder cover for games without box art: the Gobe logo as a faint watermark
 *  with the game's title and system, so the grid stays visual instead of plain text. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DefaultCover(game: Game) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF232A3D), Color(0xFF0E1119)))),
    ) {
        Image(
            painter = painterResource(R.drawable.gobe_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.62f)
                .alpha(0.16f),
        )
        Text(
            game.system.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
        Text(
            game.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(10.dp),
        )
    }
}

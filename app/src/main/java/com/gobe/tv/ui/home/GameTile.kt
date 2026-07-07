package com.gobe.tv.ui.home

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.domain.Game
import com.gobe.tv.ui.art.GameCover

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
                GameCover(game, Modifier.fillMaxSize())

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

                if (game.recommended) {
                    Text(
                        "★",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD54F),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }

                if (game.favorite) {
                    Text(
                        "♥",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE53935),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
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

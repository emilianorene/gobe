package com.gobe.tv.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.domain.Game
import com.gobe.tv.ui.art.GameCover
import com.gobe.tv.ui.theme.GobeAccentFavorites

/** Rail-row subtitle: "year · genre", omitting each part when absent. Pure for unit testing. */
fun rowSubtitle(year: Int?, genre: String?): String {
    val y = if (year != null && year > 0) year.toString() else null
    val g = genre?.trim()?.takeIf { it.isNotBlank() }
    return listOfNotNull(y, g).joinToString(" · ")
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GameRow(
    game: Game,
    onClick: () -> Unit, // A = launch directly
    onFocused: () -> Unit, // focus moved here → parent updates the panel
    requestInitialFocus: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Mirrors GameTile's approach: own the FocusRequester and request focus from within this
    // composable once it enters composition, so the requester is attached to a real node.
    val focusRequester = remember { FocusRequester() }
    if (requestInitialFocus) {
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    }
    val focusModifier = if (requestInitialFocus) Modifier.focusRequester(focusRequester) else Modifier

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(focusModifier)
            .onFocusChanged { if (it.isFocused) onFocused() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameCover(
                game,
                Modifier.width(46.dp).height(62.dp).clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    game.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                rowSubtitle(game.year, game.genre).takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                game.recommended -> Text("★", color = Color(0xFFFFD54F))
                game.favorite -> Text("♥", color = GobeAccentFavorites)
            }
        }
    }
}

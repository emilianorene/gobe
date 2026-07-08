package com.gobe.tv.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.R
import com.gobe.tv.domain.Game
import com.gobe.tv.ui.art.GameCover

/**
 * Presentational game-detail content: cover + metadata + actions. Owns no launch/DB/save-state
 * logic — the caller (DetailScreen, and later the library master-detail pane) supplies state via
 * params and reacts to taps via callbacks.
 */
@Composable
fun GameDetailPanel(
    game: Game,
    playable: Boolean,
    hasState: Boolean,
    favorite: Boolean,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onToggleFavorite: () -> Unit,
    playFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        // LEFT: large cover art
        GameCover(
            game,
            Modifier
                .width(220.dp)
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp)),
        )

        Spacer(Modifier.width(48.dp))

        // RIGHT: metadata + actions. fillMaxHeight lets the weighted description below absorb the
        // slack and keep the action buttons anchored at the bottom, always visible.
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(game.displayName, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                game.system.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.detail_players) + ": " + (game.players?.toString() ?: "—"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.detail_genre) + ": " + (game.genre ?: "—"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.detail_year) + ": " + (game.year?.toString() ?: "—"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            if (game.recommended) {
                Text(
                    "★ " + stringResource(R.string.game_recommended),
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color(0xFFFFD54F),
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                stringResource(R.string.detail_size_kb, (game.sizeBytes / 1024).toInt()) +
                    if (hasState) " · " + stringResource(R.string.detail_save_state_present) else "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Description takes the remaining height via weight(1f), so it fills a tall pane
            // (full-screen DetailScreen) and shrinks in a short one (master-detail) while the action
            // buttons below stay anchored and fully visible. maxLines is required for the weighted
            // Text to lay out and ellipsize; the weight caps the actual visible line count to fit.
            if (!game.description.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    game.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 12,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            if (playable) {
                Button(
                    onClick = onPlay,
                    modifier = playFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                ) { Text("▶ " + stringResource(R.string.detail_play)) }
                if (hasState) {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onResume) { Text("⟳ " + stringResource(R.string.detail_resume_save)) }
                }
            } else {
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = playFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                ) { Text("▶ " + stringResource(R.string.detail_play_soon)) }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onToggleFavorite) {
                Text(
                    (if (favorite) "♥ " else "♡ ") +
                        stringResource(if (favorite) R.string.detail_unfavorite else R.string.detail_favorite),
                )
            }
            footer()
        }
    }
}

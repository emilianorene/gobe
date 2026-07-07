package com.gobe.tv.ui.art

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.gobe.tv.R
import com.gobe.tv.data.art.coverUrl
import com.gobe.tv.domain.Game

/** Cover art for a game: loads coverUrl(), falling back to a branded placeholder. Fills its box. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GameCover(game: Game, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val url = coverUrl(game)
    Box(modifier) {
        if (url != null) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = game.displayName,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (painter.state is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent()
                else DefaultCover(game)
            }
        } else {
            DefaultCover(game)
        }
    }
}

/** Branded placeholder cover for games without box art (Gobe logo watermark + title/system). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DefaultCover(game: Game) {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF232A3D), Color(0xFF0E1119)))),
    ) {
        Image(
            painter = painterResource(R.drawable.gobe_logo),
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.62f).alpha(0.16f),
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
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp),
        )
    }
}

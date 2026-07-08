package com.gobe.tv.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val PlateLight = Color(0xFFF6F7F9)
private val PlateDark = Color(0xFFE6E8EE)

/** Controller art on a light "display plate" with an accent glow. Photos are multiply-blended so
 *  their white background disappears into the plate; the Arcade vector draws straight. */
@Composable
fun ConsoleHero(art: ConsoleArt, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(PlateLight, PlateDark)))
                .border(3.dp, art.accent.copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (art.isPhoto) {
                val bmp: ImageBitmap = ImageBitmap.imageResource(art.art)
                Canvas(Modifier.fillMaxSize().padding(28.dp)) {
                    val scale = min(size.width / bmp.width, size.height / bmp.height)
                    val w = (bmp.width * scale); val h = (bmp.height * scale)
                    val left = ((size.width - w) / 2f); val top = ((size.height - h) / 2f)
                    // Fill the plate area then multiply the photo over it (white -> plate).
                    drawRect(PlateLight)
                    drawImage(
                        image = bmp,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bmp.width, bmp.height),
                        dstOffset = IntOffset(left.toInt(), top.toInt()),
                        dstSize = IntSize(w.toInt(), h.toInt()),
                        blendMode = BlendMode.Multiply,
                    )
                }
            } else {
                Image(
                    painter = painterResource(art.art),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(0.7f),
                )
            }
        }
    }
}

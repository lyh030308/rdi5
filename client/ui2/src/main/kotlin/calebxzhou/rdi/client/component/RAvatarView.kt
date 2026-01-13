package calebxzhou.rdi.client.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sun.jna.platform.unix.X11
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

private const val SKIN_HEAD_U = 8
private const val SKIN_HEAD_V = 8
private const val SKIN_HEAD_SIZE = 8
private const val SKIN_HAT_U = 40
private const val SKIN_HAT_V = 8
private const val SKIN_TEX_WIDTH = 64
private const val SKIN_TEX_HEIGHT = 64

@Composable
fun RAvatarView(
    name: String,
    skinUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    backgroundColor: Color = Color(0xFF2E7D32),
    onClick: (() -> Unit)? = null,
    fallback: @Composable BoxScope.() -> Unit = {
        Text(
            text = name.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
) {
    var head by remember(skinUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(skinUrl) {
        head = skinUrl?.let { loadSkinHead(it) }
    }

    val clickModifier = if (onClick == null) {
        modifier
    } else {
        modifier.clickable(onClick = onClick)
    }

    Box(
        modifier = clickModifier
            .size(size)
            .background(backgroundColor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (head != null) {
            Image(
                bitmap = head!!,
                contentDescription = "Avatar",
                modifier = Modifier.size(size),
                filterQuality = FilterQuality.None
            )
        } else {
            fallback()
        }
    }
}

private suspend fun loadSkinHead(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bytes = URL(url).openStream().use { it.readBytes() }
        val image = Image.makeFromEncoded(bytes)
        val processed = processLegacySkin(image)
        extractHead(processed)
    }.getOrNull()
}

private fun processLegacySkin(image: Image): Image {
    if (image.width == SKIN_TEX_WIDTH && image.height == 32) {
        val surface = Surface.makeRasterN32Premul(SKIN_TEX_WIDTH, SKIN_TEX_HEIGHT)
        surface.canvas.clear(0x00000000)
        surface.canvas.drawImage(image, 0f, 0f)
        return surface.makeImageSnapshot()
    }
    return image
}

private fun extractHead(image: Image): ImageBitmap {
    val headRect = Rect.makeXYWH(
        SKIN_HEAD_U.toFloat(),
        SKIN_HEAD_V.toFloat(),
        SKIN_HEAD_SIZE.toFloat(),
        SKIN_HEAD_SIZE.toFloat()
    )
    val hatRect = Rect.makeXYWH(
        SKIN_HAT_U.toFloat(),
        SKIN_HAT_V.toFloat(),
        SKIN_HEAD_SIZE.toFloat(),
        SKIN_HEAD_SIZE.toFloat()
    )
    val dstRect = Rect.makeXYWH(0f, 0f, SKIN_HEAD_SIZE.toFloat(), SKIN_HEAD_SIZE.toFloat())

    val surface = Surface.makeRasterN32Premul(SKIN_HEAD_SIZE, SKIN_HEAD_SIZE)
    surface.canvas.clear(0x00000000)
    surface.canvas.drawImageRect(image, headRect, dstRect)
    surface.canvas.drawImageRect(image, hatRect, dstRect)

    return surface.makeImageSnapshot().asImageBitmap()
}

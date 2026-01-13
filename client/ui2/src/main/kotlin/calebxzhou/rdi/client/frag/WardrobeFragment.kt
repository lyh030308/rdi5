package calebxzhou.rdi.client.frag

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.NavState
import calebxzhou.rdi.client.SERVER_URL
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.common.model.Response
import calebxzhou.rdi.common.net.httpRequest
import calebxzhou.rdi.common.serdesJson
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.skia.Image as SkiaImage

private val logger = KotlinLogging.logger {}

@Serializable
data class ApiResponse(
    val current_page: Int,
    val data: List<SkinData>
)

@Serializable
data class SkinData(
    val tid: Int,
    val name: String,
    val type: String,
    val uploader: Int,
    val public: Boolean,
    val likes: Int
) {
    val isCape: Boolean
        get() = type == "cape"
    val isSlim: Boolean
        get() = type == "steve"
}

@Serializable
data class Skin(
    val tid: Int,
    val name: String,
    val type: String,
    val hash: String,
    val size: Int,
    val uploader: Int,
    val public: Boolean,
    val upload_at: String,
    val likes: Int
)

@Composable
fun WardrobeFragment(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    var searchText by remember { mutableStateOf("") }
    var capeMode by remember { mutableStateOf(false) }
    var skins by remember { mutableStateOf<List<SkinData>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var hasMoreData by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var selectedSkin by remember { mutableStateOf<SkinData?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    val imageCache = remember { ConcurrentHashMap<String, ImageBitmap?>() }

    fun refreshSkins() {
        if (loading) return
        loading = true
        page = 1
        hasMoreData = true
        statusMessage = null
        scope.launch {
            val result = runCatching { querySkins(page, searchText, capeMode) }
            result.onFailure { err ->
                logger.error(err) { "Query skins failed" }
                statusMessage = "加载失败${err.message ?: "未知错误"}"
            }.onSuccess { list ->
                skins = list
                if (list.isEmpty()) {
                    statusMessage = "没有找到相关皮肤"
                }
            }
            loading = false
        }
    }

    fun loadMoreSkins() {
        if (loading || !hasMoreData) return
        loading = true
        val nextPage = page + 1
        scope.launch {
            val result = runCatching { querySkins(nextPage, searchText, capeMode) }
            result.onFailure { err ->
                logger.error(err) { "Load more skins failed" }
                statusMessage = "加载失败${err.message ?: "未知错误"}"
            }.onSuccess { list ->
                if (list.isNotEmpty()) {
                    skins = skins + list
                    page = nextPage
                } else {
                    hasMoreData = false
                    statusMessage = "没有更多皮肤了"
                }
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshSkins()
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 1
        }
    }

    LaunchedEffect(shouldLoadMore, hasMoreData, loading) {
        if (shouldLoadMore && hasMoreData && !loading) {
            loadMoreSkins()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource("/assets/textures/bg/1.jpg"),
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        val panelRatio = 820f / 560f
        val maxPanelWidth = maxWidth * 0.92f
        val maxPanelHeight = maxHeight * 0.88f
        val panelWidth = minOf(maxPanelWidth, maxPanelHeight * panelRatio)
        val panelHeight = panelWidth / panelRatio

        Surface(
            modifier = Modifier
                .width(panelWidth)
                .height(panelHeight)
                .align(Alignment.Center),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "<",
                        style = MaterialTheme.typography.h5,
                        color = Color.White,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onBack() }
                    )
                    Text(
                        text = "衣柜",
                        style = MaterialTheme.typography.h6,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("搜索...", color = Color(0xFF34C759)) },
                        modifier = Modifier
                            .weight(1f)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                                    refreshSkins()
                                    true
                                } else {
                                    false
                                }
                            },
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            cursorColor = Color.White
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = capeMode,
                            onCheckedChange = { capeMode = it },
                            enabled = !loading
                        )
                        Text(text = "披风", color = Color.White)
                    }
                    Button(
                        onClick = { NavState.currentScreen.value = "mojangSkin" },
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF8D6E63),
                            contentColor = Color.White
                        )
                    ) {
                        Text("导入正版")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    modifier = Modifier.weight(1f),
                    state = gridState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(skins, key = { it.tid }) { skin ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier.width(170.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SkinPreviewImage(
                                    url = skinPreviewUrl(skin),
                                    cache = imageCache,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = skin.name.take(4),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "\u2665${skin.likes}",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                statusMessage?.let { msg ->
                    Text(text = msg, color = Color(0xFFFF7043))
                }
            }
        }
    }

    if (showConfirm && selectedSkin != null) {
        val skin = selectedSkin
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("设置确认") },
            text = { Text("要设定${if (skin?.isCape == true) "披风" else "皮肤"} ${skin?.name}吗？") },
            confirmButton = {
                Button(onClick = {
                    showConfirm = false
                    skin?.let {
                        scope.launch {
                            setSkin(it) { status ->
                                statusMessage = status
                            }
                        }
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(onClick = { showConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private suspend fun querySkins(page: Int, keyword: String, cape: Boolean): List<SkinData> {
    val urlPrefix = "https://littleskin.cn"
    val datas = mutableListOf<SkinData>()
    val startPage = (page - 1) * 2 + 1
    for (subpage in 0..1) {
        val currentPage = startPage + subpage
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val url = "$urlPrefix/skinlib/list?filter=${if (cape) "cape" else "skin"}&sort=likes&page=$currentPage&keyword=$encodedKeyword"
        val response = httpRequest {
            url(url)
            method = HttpMethod.Get
        }
        if (response.status.value in 200..299) {
            val body = response.bodyAsText()
            val skinData = serdesJson.decodeFromString<ApiResponse>(body).data
            datas.addAll(skinData)
        }
        if (subpage < 1) {
            delay(300)
        }
    }
    return datas
}

@Composable
private fun SkinPreviewImage(
    url: String,
    cache: MutableMap<String, ImageBitmap?>,
    modifier: Modifier = Modifier
) {
    var image by remember(url) { mutableStateOf(cache[url]) }

    LaunchedEffect(url) {
        if (image == null) {
            image = loadImageBitmap(url)
            cache[url] = image
        }
    }

    if (image != null) {
        Image(
            bitmap = image!!,
            contentDescription = "Skin Preview",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "加载中",
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun skinPreviewUrl(skin: SkinData): String {
    val base = "https://littleskin.cn"
    return if (skin.isCape) {
        "$base/preview/cape/${skin.tid}"
    } else {
        "$base/preview/${skin.tid}"
    }
}

private suspend fun loadImageBitmap(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bytes = URL(url).openStream().use { it.readBytes() }
        SkiaImage.makeFromEncoded(bytes).asImageBitmap()
    }.getOrNull()
}

private suspend fun setSkin(skinData: SkinData, onStatus: (String) -> Unit) {
    val urlPrefix = "https://littleskin.cn"
    val jwt = NavState.currentJwt.value
    if (jwt.isBlank()) {
        onStatus("登录已失效，请重新登录")
        return
    }
    val response = httpRequest {
        url("$urlPrefix/texture/${skinData.tid}")
        method = HttpMethod.Get
    }
    if (response.status.value !in 200..299) {
        onStatus("获取皮肤详情失败")
        return
    }
    val skin = serdesJson.decodeFromString<Skin>(response.bodyAsText())
    val cloth = if (skinData.isCape) {
        RAccount.Cloth(cape = "$urlPrefix/textures/${skin.hash}")
    } else {
        RAccount.Cloth(isSlim = skinData.isSlim, skin = "$urlPrefix/textures/${skin.hash}")
    }

    val updateResponse = httpRequest {
        url("$SERVER_URL/player/skin")
        method = HttpMethod.Post
        header(HttpHeaders.Authorization, "Bearer $jwt")
        setBody(
            MultiPartFormDataContent(
                formData {
                    append("isSlim", cloth.isSlim.toString())
                    append("skin", cloth.skin)
                    cloth.cape?.let { append("cape", it) }
                }
            )
        )
    }
    val parsed = serdesJson.decodeFromString<Response<Unit>>(updateResponse.bodyAsText())
    if (parsed.ok) {
        onStatus("皮肤设置成功（半小时或重启后可见）")
    } else {
        onStatus("皮肤设置失败, ${parsed.msg}")
    }
}

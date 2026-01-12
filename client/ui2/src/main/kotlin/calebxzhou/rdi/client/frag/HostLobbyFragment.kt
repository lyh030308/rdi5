package calebxzhou.rdi.client.frag

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.NavState
import calebxzhou.rdi.client.SERVER_URL
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.common.model.Response
import calebxzhou.rdi.common.net.httpRequest
import calebxzhou.rdi.common.serdesJson
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch
import org.bson.types.ObjectId

private const val USE_MOCK_DATA = false
private val logger = KotlinLogging.logger {}

@Composable
fun HostLobbyFragment(
    onBack: () -> Unit,
    onOpenHostInfo: (Host.Vo) -> Unit = {},
    onStartHost: (Host.Vo) -> Unit = {},
    onWorldList: () -> Unit = {},
    onCarrier: () -> Unit = {},
    onModpackList: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var hosts by remember { mutableStateOf<List<Host.Vo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showInvitedOnly by remember { mutableStateOf(true) }

    fun loadHosts(my: Boolean) {
        if (isLoading) return
        isLoading = true
        statusMessage = null
        scope.launch {
            val result = runCatching {
                if (USE_MOCK_DATA) {
                    generateMockHosts()
                } else {
                    fetchHosts(my)
                }
            }
            result.onFailure { err ->
                logger.error(err) { "Load hosts failed" }
                statusMessage = "加载失败${err.message ?: "未知错误"}"
            }.onSuccess { list ->
                hosts = list
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadHosts(showInvitedOnly)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource("/assets/textures/bg/1.jpg"),
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            modifier = Modifier
                .width(820.dp)
                .height(580.dp)
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
                        text = "大厅",
                        style = MaterialTheme.typography.h6,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "选择你要游玩的服务器。你可以选择官服，也可以选择其他玩家的自建服",
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showInvitedOnly,
                            onCheckedChange = { checked ->
                                showInvitedOnly = checked
                                loadHosts(checked)
                            },
                            enabled = !isLoading
                        )
                        Text(
                            text = "我受邀的",
                            color = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    QuickActionButton(
                        label = "MC状态",
                        color = Color(0xFF00897B),
                        enabled = !isLoading,
                        onClick = {
                            statusMessage = "请先选择服务器并启动MC后再查看状态"
                        }
                    )
                    QuickActionButton(
                        label = "存档",
                        color = Color(0xFF1E88E5),
                        enabled = !isLoading,
                        onClick = onWorldList
                    )
                    QuickActionButton(
                        label = "节点",
                        enabled = !isLoading,
                        onClick = onCarrier
                    )
                    QuickActionButton(
                        label = "选包开服",
                        color = Color(0xFF43A047),
                        enabled = !isLoading,
                        onClick = onModpackList
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hosts.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isLoading) "加载中..." else "暂无可展示的主机",
                                color = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        hosts.forEach { host ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Black.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = host.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "服主：${host.ownerName} 端口：${host.port}",
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = { onOpenHostInfo(host) },
                                            enabled = !isLoading,
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = Color(0xFF5D4037),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("查看详情")
                                        }
                                        Button(
                                            onClick = { onStartHost(host) },
                                            enabled = !isLoading,
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = Color(0xFF2E7D32),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("加入游戏")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    color: Color = Color(0xFF2E7D32),
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = color,
            contentColor = Color.White
        )
    ) {
        Text(label)
    }
}

private suspend fun fetchHosts(my: Boolean): List<Host.Vo> {
    val response = httpRequest {
        url("$SERVER_URL/host/${if (my) "my" else "lobby/0"}")
        method = HttpMethod.Get
        withAuth()
    }
    val bodyText = response.bodyAsText()
    val parsed = serdesJson.decodeFromString<Response<List<Host.Vo>>>(bodyText)
    if (!parsed.ok) {
        error(parsed.msg.ifBlank { "加载失败" })
    }
    return parsed.data.orEmpty()
}

private fun generateMockHosts(): List<Host.Vo> = List(12) { index ->
    val base = Host.Vo.TEST
    base.copy(
        _id = ObjectId(),
        name = "${base.name} #${index + 1}",
        ownerName = "${base.ownerName} #${index + 1}",
        port = base.port + index
    )
}

private fun io.ktor.client.request.HttpRequestBuilder.withAuth() {
    val jwt = NavState.currentJwt.value
    if (jwt.isNotBlank()) {
        header(HttpHeaders.Authorization, "Bearer $jwt")
    }
}


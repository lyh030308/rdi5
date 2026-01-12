package calebxzhou.rdi.client.frag

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.model.McVersion
import kotlinx.coroutines.launch

@Composable
fun McVersionManageFragment(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("准备就绪") }
    var isBusy by remember { mutableStateOf(false) }

    fun runTask(label: String, task: suspend (onProgress: (String) -> Unit) -> Unit) {
        if (isBusy) return
        isBusy = true
        statusText = "$label 开始"
        scope.launch {
            val result = runCatching {
                task { statusText = it }
            }
            statusText = if (result.isSuccess) {
                "$label 完成"
            } else {
                "$label 失败：${result.exceptionOrNull()?.message ?: "未知错误"}"
            }
            isBusy = false
        }
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
                .width(720.dp)
                .height(520.dp)
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
                        text = "MC版本资源管理",
                        style = MaterialTheme.typography.h6,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    McVersion.entries.forEach { mcver ->
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${mcver.mcVer}：",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = {
                                        runTask("下载MC文件") { onProgress ->

                                            val firstLoader = mcver.loaderVersions.keys.firstOrNull()
                                            if (firstLoader != null) {

                                            }
                                        }
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF2196F3),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("下载全部所需文件")
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SmallActionButton(
                                        label = "仅下载MC核心",
                                        enabled = !isBusy,
                                        onClick = {
                                            statusText = "该操作需要完整版本清单支持，待补全实现"
                                        }
                                    )
                                    SmallActionButton(
                                        label = "仅下载运行库",
                                        enabled = !isBusy,
                                        onClick = {
                                            statusText = "该操作需要完整版本清单支持，待补全实现"
                                        }
                                    )
                                    SmallActionButton(
                                        label = "仅下载音频资源",
                                        enabled = !isBusy,
                                        onClick = {
                                            statusText = "该操作需要完整版本清单支持，待补全实现"
                                        }
                                    )
                                }
                                if (mcver.loaderVersions.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        mcver.loaderVersions.keys.forEach { loader ->
                                            SmallActionButton(
                                                label = "仅安装${loader.name.lowercase()}",
                                                enabled = !isBusy,
                                                onClick = {
                                                    runTask("下载${loader}文件") { onProgress ->

                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF4CAF50),
            contentColor = Color.White
        ),

    ) {
        Text(label)
    }
}


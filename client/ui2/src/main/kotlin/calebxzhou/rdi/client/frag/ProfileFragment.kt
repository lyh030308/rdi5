package calebxzhou.rdi.client.frag

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileFragment(
    userId: String,
    hasMcResources: Boolean,
    onLobby: () -> Unit,
    onMcAssets: () -> Unit,
    onMail: () -> Unit,
    onLogout: () -> Unit,
    onChangeProfile: () -> Unit,
    onWardrobe: () -> Unit,
    hwSpec: Map<String, String> = emptyMap()
) {
    val errorMessage = remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource("assets/textures/bg/1.jpg"),
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .width(560.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "我的信息",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onMcAssets,
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("\uDB84\uDE5FMC版本资源")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onMail,
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1E88E5),
                            contentColor = Color.White
                        )
                    ) {
                        Text("\uEB1C信箱")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onLogout,
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Text("\uDB83\uDFC5登出")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF2E7D32), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userId.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Column {
                        Text(text = "ID: $userId", color = Color.White.copy(alpha = 0.85f))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onChangeProfile,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF6D4C41),
                            contentColor = Color.White
                        )
                    ) {
                        Text("修改资料")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onWardrobe,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF00897B),
                            contentColor = Color.White
                        )
                    ) {
                        Text("衣柜")
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "硬件信息",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (hwSpec.isEmpty()) {
                            Text(
                                text = "暂无硬件信息",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        } else {
                            hwSpec.forEach { (key, value) ->
                                Text(
                                    text = "$key: $value",
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (hasMcResources) {
                        errorMessage.value = null
                        onLobby()
                    } else {
                        errorMessage.value =
                            "你还没有完整下载MC文件，必须下载才能游玩RDI，可点击上方“MC版本资源”按钮下载。"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (hasMcResources) Color(0xFF2E7D32) else Color(0xFF455A64),
                    contentColor = Color.White
                )
            ) {
                Text("▶ 大厅")
            }
            Spacer(modifier = Modifier.height(8.dp))
            errorMessage.value?.let { msg ->
                Text(text = msg, color = Color(0xFFFF7043))
            }
        }
    }
}

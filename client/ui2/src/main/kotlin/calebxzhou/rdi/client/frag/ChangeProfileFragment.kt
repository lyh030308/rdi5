package calebxzhou.rdi.client.frag

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.NavState
import calebxzhou.rdi.client.net.RServer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch

@Composable
fun ChangeProfileFragment(
    initialName: String,
    onBack: () -> Unit
) {
    val logger = KotlinLogging.logger {}
    val scope = rememberCoroutineScope()
    val nameState = rememberTextFieldState(initialName)
    val passwordState = rememberTextFieldState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource("/assets/textures/bg/1.jpg"),
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            modifier = Modifier
                .width(420.dp)
                .height(360.dp)
                .align(Alignment.Center),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
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
                        text = "修改信息",
                        style = MaterialTheme.typography.h6,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    state = nameState,
                    label = { Text("昵称", color = Color(0xFF34C759)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    state = passwordState,
                    label = { Text("新密码 留空则不修改", color = Color(0xFF34C759)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            errorMessage = null
                            successMessage = null

                            val name = nameState.text.toString()
                            val pwd = passwordState.text.toString()
                            val nameToSend = name.takeIf { it != initialName }
                            val pwdToSend = pwd.takeIf { it.isNotBlank() }

                            val nameBytes = name.toByteArray(Charsets.UTF_8).size
                            if (nameBytes !in 3..24) {
                                errorMessage = "昵称须在3~24个字节，当前为${nameBytes}"
                                return@Button
                            }
                            if (pwd.isNotEmpty() && pwd.length !in 6..16) {
                                errorMessage = "密码长度须在6~16个字符"
                                return@Button
                            }
                            if (nameToSend == null && pwdToSend == null) {
                                errorMessage = "没有需要修改的内容"
                                return@Button
                            }
                            if (NavState.currentJwt.value.isBlank()) {
                                errorMessage = "登录已失效，请重新登录"
                                return@Button
                            }

                            scope.launch {
                                val result = runCatching {
                                    RServer.updateProfile(nameToSend, pwdToSend, NavState.currentJwt.value)
                                }
                                result.onFailure { err ->
                                    logger.error(err) { "Update profile request failed" }
                                    errorMessage = "修改失败${err.message ?: "未知错误"}"
                                }.onSuccess { response ->
                                    val parsed = response.response
                                    if (response.httpStatus in 200..299 && parsed?.ok == true) {
                                        successMessage = parsed.msg.ifBlank { "修改成功" }
                                        nameToSend?.let { NavState.currentUserId.value = it }
                                        NavState.currentScreen.value = "profile"
                                    } else {
                                        errorMessage = parsed?.msg
                                            ?: "修改失败: HTTP ${response.httpStatus}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        )
                    ) {
                        Text("修改")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                errorMessage?.let { Text(it, color = Color.Red) }
                successMessage?.let { Text(it, color = Color.Green) }
            }
        }
    }
}


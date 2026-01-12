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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.net.RServer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

@Composable
fun RegisterFragment(onRegisterSuccess: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var qq by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource("/assets/textures/bg/1.jpg"),
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Surface(
            modifier = Modifier
                .width(400.dp)
                .height(450.dp)
                .align(Alignment.Center),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp) // ???????????
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "<",
                        style = MaterialTheme.typography.h5,
                        color = Color.White,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onBack() }
                    )

                    Text(
                        text = "注册新账号",
                        style = MaterialTheme.typography.h6,
                        color = Color.White
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("昵称 支持中文", color = Color.White) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = qq,
                    onValueChange = { qq = it },
                    label = { Text("QQ号", color = Color.White) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码", color = Color.White) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认密码", color = Color.White) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = {
                        errorMessage = null
                        successMessage = null
                        val trimmedUsername = username.trim()
                        val trimmedQq = qq.trim()
                        if (trimmedUsername.isEmpty() || trimmedQq.isEmpty() || password.isEmpty()) {
                            errorMessage = "请填写完整信息"
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorMessage = "两次输入的密码不一致"
                            return@Button
                        }
                        scope.launch {
                            val result = runCatching {
                                RServer.register(trimmedUsername, password, trimmedQq)
                            }
                            result.onFailure { err ->
                                logger.error(err) { "Register request failed" }
                                errorMessage = "注册失败${err.message ?: "未知错误"}"
                            }.onSuccess { response ->
                                val parsed = response.response
                                if (response.httpStatus in 200..299 && parsed?.ok == true) {
                                    successMessage = parsed.msg.ifBlank { "注册成功" }
                                    showSuccessDialog = true
                                } else {
                                    errorMessage = parsed?.msg
                                        ?: "注册失败: HTTP ${response.httpStatus}"
                                }
                            }
                        }
                    }) {
                        Text("注册")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                errorMessage?.let { Text(it, color = Color.Red) }
                successMessage?.let { Text(it, color = Color.Green) }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("注册成功") },
            text = { Text(successMessage ?: "注册成功") },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onRegisterSuccess()
                }) {
                    Text("确定")
                }
            }
        )
    }
}



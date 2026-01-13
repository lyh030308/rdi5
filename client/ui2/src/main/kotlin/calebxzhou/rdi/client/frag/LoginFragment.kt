package calebxzhou.rdi.client.frag

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.TextFieldDefaults  // Material2 版本
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import calebxzhou.rdi.client.NavState
import calebxzhou.rdi.client.component.LoadingView
import calebxzhou.rdi.client.net.RServer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.prefs.Preferences

private val logger = KotlinLogging.logger {}

@Composable
fun LoginFragment(onRegister: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = remember { Preferences.userRoot().node("calebxzhou.rdi.client.login") }
    val cachedUsername = remember { prefs.get("last_username", "") }
    val cachedPassword = remember { prefs.get("last_password", "") }
    val usernameState = rememberTextFieldState(cachedUsername)
    var password by remember { mutableStateOf(cachedPassword) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    // 定义 Nerd Font 家族
    val nerdFont = FontFamily(
        Font(
            resource = "SymbolsNerdFont-Regular.ttf",  // 直接用字符串路径，从 resources/ 加载
            weight = FontWeight.Normal
        )
    )

    val submitLogin: (String, String) -> Unit = label@{ usernameInput, passwordInput ->
        errorMessage = null
        successMessage = null
        val username = usernameInput.trim()
        val password = passwordInput
        if (username.isEmpty() || password.isEmpty()) {
            errorMessage = "请输入账号和密码"
            return@label
        }
        prefs.put("last_username", username)
        prefs.put("last_password", password)
        isLoading = true
        scope.launch {
            val result = runCatching {
                RServer.login(username, password)
            }
            result.onFailure { err ->
                logger.error(err) { "Login request failed" }
                errorMessage = "登录失败${err.message ?: "未知错误"}"
                isLoading = false
            }.onSuccess { response ->
                val parsed = response.response
                                    if (response.httpStatus in 200..299 && parsed?.ok == true) {
                                        successMessage = parsed.msg.ifBlank { "登录成功" }
                                        val displayName = parsed.data
                                            ?.jsonObject
                                            ?.get("name")
                                            ?.jsonPrimitive
                                            ?.content
                                            ?.ifBlank { username }
                                            ?: username
                                        val accountId = parsed.data
                                            ?.jsonObject
                                            ?.get("_id")
                                            ?.jsonPrimitive
                                            ?.content
                                            ?.ifBlank { displayName }
                                            ?: displayName
                                        val accountQq = parsed.data
                                            ?.jsonObject
                                            ?.get("qq")
                                            ?.jsonPrimitive
                                            ?.content
                                            ?.ifBlank { username }
                                            ?: username
                                        NavState.currentUserId.value = displayName.ifBlank { "玩家" }
                                        NavState.currentJwt.value = response.jwt.orEmpty()
                                        RServer.loggedAccount.id = accountId
                                        RServer.loggedAccount.qq = accountQq
                                        NavState.currentScreen.value = "profile"
                                    } else {
                    val serverMsg = parsed?.msg.orEmpty()
                    val isQqInput = username.all { it.isDigit() } && username.length in 5..10
                    errorMessage = if (isQqInput && serverMsg.contains("密码错误")) {
                        "密码错误"
                    } else {
                        parsed?.msg ?: "登录失败: HTTP ${response.httpStatus}"
                    }
                }
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图片
        Image(
            painter = painterResource("/assets/textures/bg/1.jpg"),
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 登录面板
        Surface(
            modifier = Modifier
                .width(400.dp)
                .height(350.dp)
                .align(Alignment.Center),
            color = Color.Black.copy(alpha = 0.6f),  // 黑色 + 高透明度
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp) // 左右多留空间，上下适中
            ) {
                // === 第一行：左上角的 （这就是红框位置）===
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\uf053",  // Unicode 码点：使用 Nerd Font 的图标（如返回箭头）。可在 https://www.nerdfonts.com/cheat-sheet 查询更多
                        color = Color.White,
                        style = MaterialTheme.typography.h6.copy(  // 复制默认样式并覆盖字体
                            fontFamily = nerdFont,
                            fontSize = 24.sp  // 调整大小以适应图标
                        ),
                        modifier = Modifier
                            .padding(12.dp)
                            .clickable { onBack() }
                    )
                    Text(
                        text = "登录",
                        style = MaterialTheme.typography.h6,
                        color = Color.White,  // 改为白色
                    )
                }

                Spacer(modifier = Modifier.height(32.dp)) // 标题和输入框之间的间距

                // === 输入框区域 ===
                OutlinedTextField(
                    state = usernameState,
                    label = { Text(
                        "RDID/QQ号",
                        color = Color(0xFF34C759)
                        ) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(
                        "密码",
                        color = Color(0xFF34C759)
                    ) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        Text(
                            text = "\uDB80\uDE08",
                            color = Color.White,
                            style = MaterialTheme.typography.h6.copy(
                                fontFamily = nerdFont,
                                fontSize = 20.sp
                            ),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { showPassword = !showPassword }
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.weight(1f)) // 把按钮推到下方

                // === 按钮区域 ===
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            submitLogin(
                                usernameState.text.toString(),
                                password
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),  // 圆角大小，12.dp 常见好看，可调成 8.dp 或 16.dp
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2196F3),  // 经典 Material Blue
                            contentColor = Color.White             // 文字颜色保持白色，对比度高
                        )
                    ) {
                        Text("登录")
                    }
                    Button(
                        onClick = onRegister,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),  // 圆角，12.dp 舒适好看
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF4CAF50),  // 经典绿色
                            contentColor = Color.White            // 文字白色，高对比度
                        )
                    ){
                        Text("注册")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                errorMessage?.let { Text(it, color = Color.Red) }
                successMessage?.let { Text(it, color = Color.Green) }
            }
        }

        LoadingView(show = isLoading)
    }
}

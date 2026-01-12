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
import androidx.compose.material.AlertDialog
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
import calebxzhou.mykotutils.std.secondsToHumanDateTime
import calebxzhou.rdi.client.NavState
import calebxzhou.rdi.client.SERVER_URL
import calebxzhou.rdi.common.model.Mail
import calebxzhou.rdi.common.model.Response
import calebxzhou.rdi.common.net.httpRequest
import calebxzhou.rdi.common.serdesJson
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import org.bson.types.ObjectId
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

@Composable
fun MailFragment(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var mails by remember { mutableStateOf<List<Mail.Vo>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<Set<ObjectId>>(emptySet()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var detailMail by remember { mutableStateOf<Mail?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    fun refreshInbox() {
        if (isLoading) return
        isLoading = true
        statusMessage = null
        scope.launch {
            val result = runCatching { fetchInbox() }
            result.onFailure { err ->
                logger.error(err) { "Load inbox failed" }
                statusMessage = "加载失败${err.message ?: "未知错误"}"
            }.onSuccess { list ->
                mails = list
                selectedIds = selectedIds.intersect(list.map { it.id }.toSet())
            }
            isLoading = false
        }
    }

    fun openDetail(mailId: ObjectId) {
        if (isLoading) return
        isLoading = true
        statusMessage = null
        scope.launch {
            val result = runCatching { fetchMailDetail(mailId) }
            result.onFailure { err ->
                logger.error(err) { "Load mail detail failed" }
                statusMessage = "加载失败${err.message ?: "未知错误"}"
            }.onSuccess { mail ->
                detailMail = mail
                showDetailDialog = true
            }
            isLoading = false
        }
    }

    fun toggleSelectAll(checked: Boolean) {
        selectedIds = if (checked) {
            mails.map { it.id }.toSet()
        } else {
            emptySet()
        }
    }

    LaunchedEffect(Unit) {
        refreshInbox()
    }

    val allSelected = mails.isNotEmpty() && selectedIds.size == mails.size

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource("/assets/textures/bg/1.jpg"),
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            modifier = Modifier
                .width(760.dp)
                .height(540.dp)
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
                        text = "信箱",
                        style = MaterialTheme.typography.h6,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { checked -> toggleSelectAll(checked) },
                            enabled = mails.isNotEmpty() && !isLoading
                        )
                        Text(
                            text = "全选",
                            color = Color.White,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Button(
                            onClick = { showDeleteConfirm = true },
                            enabled = selectedIds.isNotEmpty() && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            )
                        ) {
                            Text("删除所选")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mails.isEmpty()) {
                        Text(
                            text = "什么都没有~",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    } else {
                        mails.forEach { mail ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                                    .clickable { openDetail(mail.id) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(mail.id),
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) {
                                            selectedIds + mail.id
                                        } else {
                                            selectedIds - mail.id
                                        }
                                    },
                                    enabled = !isLoading
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "信箱 ${mail.title}",
                                        color = if (mail.unread) Color(0xFFFFF176) else Color.White,
                                        fontWeight = if (mail.unread) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${mail.intro}...",
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = "${mail.senderName} ${mail.id.timestamp.secondsToHumanDateTime}",
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除确认") },
            text = { Text("要删除所选的邮件吗？") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    if (selectedIds.isEmpty()) return@Button
                    scope.launch {
                        val result = runCatching { deleteMails(selectedIds.toList()) }
                        result.onFailure { err ->
                            logger.error(err) { "Delete mails failed" }
                            statusMessage = "删除失败${err.message ?: "未知错误"}"
                        }.onSuccess {
                            statusMessage = "已删除"
                            selectedIds = emptySet()
                            refreshInbox()
                        }
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDetailDialog && detailMail != null) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = { Text(detailMail?.title ?: "详细内容") },
            text = { Text(detailMail?.content ?: "") },
            confirmButton = {
                Button(onClick = { showDetailDialog = false }) {
                    Text("关闭")
                }
            },
            dismissButton = {
                Button(onClick = {
                    detailMail?.let { mail ->
                        scope.launch {
                            runCatching { deleteMail(mail._id) }
                                .onSuccess {
                                    statusMessage = "已删除"
                                    refreshInbox()
                                }
                                .onFailure { err ->
                                    logger.error(err) { "Delete mail failed" }
                                    statusMessage = "删除失败${err.message ?: "未知错误"}"
                                }
                        }
                    }
                    showDetailDialog = false
                }) {
                    Text("删除")
                }
            }
        )
    }
}

private suspend fun fetchInbox(): List<Mail.Vo> {
    val response = httpRequest {
        url("$SERVER_URL/mail")
        method = HttpMethod.Get
        withAuth()
    }
    val bodyText = response.bodyAsText()
    val parsed = serdesJson.decodeFromString<Response<List<Mail.Vo>>>(bodyText)
    if (!parsed.ok) {
        error(parsed.msg.ifBlank { "加载失败" })
    }
    return parsed.data.orEmpty()
}

private suspend fun fetchMailDetail(mailId: ObjectId): Mail {
    val response = httpRequest {
        url("$SERVER_URL/mail/$mailId")
        method = HttpMethod.Get
        withAuth()
    }
    val bodyText = response.bodyAsText()
    val parsed = serdesJson.decodeFromString<Response<Mail>>(bodyText)
    val data = parsed.data
    if (!parsed.ok || data == null) {
        error(parsed.msg.ifBlank { "加载失败" })
    }
    return data
}

private suspend fun deleteMail(mailId: ObjectId) {
    val response = httpRequest {
        url("$SERVER_URL/mail/$mailId")
        method = HttpMethod.Delete
        withAuth()
    }
    val bodyText = response.bodyAsText()
    val parsed = serdesJson.decodeFromString<Response<Unit>>(bodyText)
    if (!parsed.ok) {
        error(parsed.msg.ifBlank { "删除失败" })
    }
}

private suspend fun deleteMails(ids: List<ObjectId>) {
    val response = httpRequest {
        url("$SERVER_URL/mail")
        method = HttpMethod.Delete
        contentType(ContentType.Application.Json)
        setBody(ids)
        withAuth()
    }
    val bodyText = response.bodyAsText()
    val parsed = serdesJson.decodeFromString<Response<Unit>>(bodyText)
    if (!parsed.ok) {
        error(parsed.msg.ifBlank { "删除失败" })
    }
}

private fun io.ktor.client.request.HttpRequestBuilder.withAuth() {
    val jwt = NavState.currentJwt.value
    if (jwt.isNotBlank()) {
        header(HttpHeaders.Authorization, "Bearer $jwt")
    }
}

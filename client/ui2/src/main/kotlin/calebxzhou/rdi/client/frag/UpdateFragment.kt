package calebxzhou.rdi.client.frag

import calebxzhou.rdi.client.frag.LoginFragment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
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
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.ktor.downloadFileFrom
import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.mykotutils.std.sha1
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.exception.RequestError
import io.github.oshai.kotlinlogging.KotlinLogging
import calebxzhou.rdi.client.model.McVersion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess

val lgr = KotlinLogging.logger { }

@Composable
fun UpdateFragment(onSkip: () -> Unit, onContinue: () -> Unit) {
    val scope = rememberCoroutineScope()
    var progressText by remember { mutableStateOf("正在检查更新") }
    var showRetry by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }

    val uiJarFile: File = System.getProperty("rdi.jar.ui")?.let { File(it) } ?: File("rdi-5-ui.jar").normalize().absoluteFile

    LaunchedEffect(Unit) {
        startUpdateFlow(
            uiJarFile = uiJarFile,
            onProgress = { progressText = it },
            onRetry = { showRetry = true },
            onContinue = onContinue
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showLogin) {

        } else {
            Image(
                painter = painterResource("/assets/textures/bg/1.jpg"),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Surface(
                    modifier = Modifier
                        .width(400.dp)
                        .height(350.dp)
                        .align(Alignment.Center),
            color = Color.Black.copy(alpha = 0.6f),  // 黑色 + 高透明度
            shape = RoundedCornerShape(16.dp)
            ){Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = progressText, style = MaterialTheme.typography.h6)

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (showRetry) {
                        Button(onClick = {
                            showRetry = false
                            progressText = "正在检查更新"
                            scope.launch {
                                startUpdateFlow(
                                    uiJarFile = uiJarFile,
                                    onProgress = { progressText = it },
                                    onRetry = { showRetry = true },
                                    onContinue = onContinue
                                )
                            }
                        }) {
                            Text("重试更新")
                        }
                    }

                    Button(onClick = onSkip) {
                        Text("跳过更新")
                    }
                }
            }
            }


        }
    }
}

private suspend fun startUpdateFlow(
    uiJarFile: File,
    onProgress: (String) -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit
) {
    runCatching {
        val mcTargets = McVersion.entries.flatMap { mcVer ->
            mcVer.loaderVersions.keys.map { loader ->
                val slug = "${mcVer.mcVer}-${loader.name.lowercase()}"
                slug to DL_MOD_DIR.resolve("rdi-5-mc-client-$slug.jar").absoluteFile
            }
        }

        mcTargets.forEach { (slug, mcFile) ->

            val mcNeedsUpdate = false
            if (mcNeedsUpdate) {
                onProgress("准备下载 ${mcFile.name}...")
                val mcHash = ""

                val mcUpdated = false
                if (!mcUpdated) {
                    onRetry()
                    return
                }
                onProgress("${mcFile.name} 更新完成")
            }
        }


        val uiHash = null
        val uiNeedsUpdate = !uiJarFile.exists() || uiJarFile.sha1 != uiHash

        if (uiNeedsUpdate) {
            onProgress("准备下载 ${uiJarFile.name}...")

            return
        }

        lgr.info { "是最新版核心" }
        onProgress("当前已是最新版核心")
        onContinue()
    }.onFailure {
        lgr.error(it) { "核心更新流程失败" }
        onProgress("更新流程遇到错误：${it.message ?: it::class.simpleName}")
        onRetry()
    }
}

private suspend fun downloadAndReplaceCore(
    targetFile: File,
    downloadUrl: String,
    expectedSha: String,
    label: String,
    onProgress: (String) -> Unit
): Boolean {
    val parentDir = targetFile.absoluteFile.parentFile ?: File(".")
    if (!parentDir.exists()) parentDir.mkdirs()
    val tempFile = File(parentDir, "${targetFile.name}.downloading.${System.currentTimeMillis()}")

    tempFile.toPath().downloadFileFrom(downloadUrl) { dl ->
        val totalBytes = dl.totalBytes
        val downloadedBytes = dl.bytesDownloaded.takeIf { it >= 0 } ?: 0L
        val percentValue = when {
            totalBytes > 0 -> downloadedBytes * 100.0 / totalBytes
            dl.percent >= 0 -> dl.percent.toDouble()
            else -> -1.0
        }

        val percentText = percentValue.takeIf { it >= 0 }
            ?.let { String.format("%.1f%%", it) } ?: "--"
        val downloadedText = downloadedBytes.takeIf { it > 0 }?.humanFileSize ?: "0B"
        val totalText = totalBytes.takeIf { it > 0 }?.humanFileSize ?: "--"
        val speedText = dl.speedBytesPerSecond.takeIf { it > 0 }
            ?.let { "${it / 1000}KB/s" } ?: "--"

        onProgress("下载中：$percentText $downloadedText/$totalText $speedText")
    }.getOrElse {
        tempFile.delete()
        onProgress("下载失败，请检查网络后重试。${it.message}")
        return false
    }

    val downloadedSha = tempFile.sha1
    if (!downloadedSha.equals(expectedSha, true)) {
        tempFile.delete()
        onProgress("文件损坏了，请重下")
        return false
    }

    suspend fun deleteWithRetry(file: File, retries: Int = 5, delayMs: Long = 200): Boolean {
        repeat(retries) { _ ->
            if (!file.exists() || file.delete()) return true
            delay(delayMs)
        }
        return !file.exists()
    }




    fun tryOverwriteEvenIfLocked(src: File, dst: File): Boolean = runCatching {
        FileInputStream(src).channel.use { inCh ->
            FileOutputStream(dst, false).channel.use { outCh ->
                outCh.truncate(0)
                var pos = 0L
                val size = inCh.size()
                while (pos < size) {
                    val transferred = inCh.transferTo(pos, 1024 * 1024, outCh)
                    if (transferred <= 0) break
                    pos += transferred
                }
            }
        }
        true
    }.getOrElse { false }

    val backupFile = if (targetFile.exists()) File(
        parentDir,
        "${targetFile.name}.backup.${System.currentTimeMillis()}"
    ) else null

    val replaced = runCatching {
        backupFile?.let { targetFile.copyTo(it, overwrite = true) }

        if (targetFile.exists() && !deleteWithRetry(targetFile)) {
            targetFile.deleteOnExit()
            // attempt overwrite without delete
            val overwritten = tryOverwriteEvenIfLocked(tempFile, targetFile)
            if (!overwritten) {
                throw IllegalStateException("无法删除旧文件: ${targetFile.absolutePath}")
            }
            tempFile.delete()
            return@runCatching
        }

        Files.move(
            tempFile.toPath(),
            targetFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }.recoverCatching {
        // Move may fail on some filesystems; fallback to copy + delete or overwrite
        if (targetFile.exists() && !deleteWithRetry(targetFile)) {
            targetFile.deleteOnExit()
            if (!tryOverwriteEvenIfLocked(tempFile, targetFile)) {
                throw IllegalStateException("无法删除旧文件: ${targetFile.absolutePath}")
            }
            tempFile.delete()
        } else {
            Files.copy(
                tempFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            tempFile.delete()
        }
    }.onFailure { err ->
        onProgress("替换核心文件失败：${err.message}")
        tempFile.delete()
        backupFile?.let { backup ->
            runCatching {
                if (!targetFile.exists() && backup.exists()) {
                    Files.move(
                        backup.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        }
    }.isSuccess

    if (replaced) {
        backupFile?.delete()
        onProgress("核心文件已更新至最新版本。")
    }

    return replaced
}

private fun startRestartCountdown(onProgress: (String) -> Unit) {
    exitProcess(0)
    /*for (i in 5 downTo 1) {
        onProgress("更新完成，客户端将在 $i 秒后重启...")
        kotlinx.coroutines.delay(1000)
    }
    onProgress("正在重启客户端...")
    exitProcess(0)*/
}
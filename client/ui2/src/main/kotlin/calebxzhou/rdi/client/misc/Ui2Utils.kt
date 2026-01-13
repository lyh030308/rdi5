package calebxzhou.rdi.client.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.SwingUtilities
import kotlin.math.roundToInt
import oshi.SystemInfo



fun uiThread(run: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) {
        run()
    } else {
        SwingUtilities.invokeLater(run)
    }
}

fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}

fun collectHardwareSpec(): Map<String, String> {
    val systemInfo = SystemInfo()
    val hardware = systemInfo.hardware
    val os = systemInfo.operatingSystem
    val spec = LinkedHashMap<String, String>()

    val computerSystem = hardware.computerSystem
    val model = listOfNotNull(
        computerSystem.manufacturer.takeIf { it.isNotBlank() },
        computerSystem.model.takeIf { it.isNotBlank() }
    ).joinToString(" ")
    if (model.isNotBlank()) {
        spec["机型"] = model
    }

    val osVersion = listOfNotNull(
        os.family.takeIf { it.isNotBlank() },
        os.versionInfo?.version?.takeIf { it.isNotBlank() }
    ).joinToString(" ")
    if (osVersion.isNotBlank()) {
        spec["系统"] = osVersion
    }

    val cpu = hardware.processor
    val cpuName = cpu.processorIdentifier?.name?.takeIf { it.isNotBlank() } ?: "Unknown CPU"
    val maxFreqGHz = cpu.maxFreq.takeIf { it > 0 }?.let { hz ->
        val ghz = hz / 1_000_000_000.0
        String.format("%.2f GHz", ghz)
    }
    spec["CPU"] = listOfNotNull(cpuName, maxFreqGHz).joinToString(" ")

    val memoryGb = hardware.memory.total.toDouble() / (1024 * 1024 * 1024)
    spec["内存"] = String.format("%.1f GB", memoryGb)

    val gpus = hardware.graphicsCards
    if (gpus.isEmpty()) {
        spec["显卡"] = "Unknown"
    } else {
        gpus.forEachIndexed { index, gpu ->
            val vramGb = gpu.vRam.takeIf { it > 0 }?.let { it.toDouble() / (1024 * 1024 * 1024) }
            val vramText = vramGb?.let { String.format(" %.0f GB", it) }.orEmpty()
            spec["显卡${index + 1}"] = "${gpu.name}$vramText"
        }
    }

    val disks = hardware.diskStores
    if (disks.isNotEmpty()) {
        disks.forEachIndexed { index, disk ->
            val sizeGb = disk.size.toDouble() / (1024 * 1024 * 1024)
            spec["磁盘${index + 1}"] = "${disk.model} ${String.format("%.0f GB", sizeGb)}"
        }
    }

    val devices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    devices.forEachIndexed { index, device ->
        val mode = device.displayMode
        val refresh = mode.refreshRate.takeIf { it > 0 } ?: 0
        val refreshText = if (refresh > 0) "${refresh}Hz" else ""
        val resolution = "${mode.width}x${mode.height}$refreshText"
        spec["显示器${index + 1}"] = resolution
    }

    if (spec.isEmpty()) {
        spec["硬件信息"] = "无法获取"
    }

    return spec
}

@Composable
fun headButton(
    userId: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    backgroundColor: Color = Color(0xFF2E7D32),
    onClick: () -> Unit = {},
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content ?: {
            Text(
                text = userId.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    )
}


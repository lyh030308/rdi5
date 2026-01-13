package calebxzhou.rdi.client

import calebxzhou.rdi.client.frag.ChangeProfileFragment
import calebxzhou.rdi.client.frag.HostLobbyFragment
import calebxzhou.rdi.client.frag.LoginFragment
import calebxzhou.rdi.client.frag.MailFragment
import calebxzhou.rdi.client.frag.McVersionManageFragment
import calebxzhou.rdi.client.frag.MojangSkinFragment
import calebxzhou.rdi.client.frag.ProfileFragment
import calebxzhou.rdi.client.frag.RegisterFragment
import calebxzhou.rdi.client.frag.WardrobeFragment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import calebxzhou.rdi.client.frag.TitleFragment
import calebxzhou.rdi.client.frag.UpdateFragment
import calebxzhou.rdi.client.misc.collectHardwareSpec
import kotlinx.coroutines.Dispatchers.Main

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "RDI dev") {
        App()
    }
}

@Composable
fun App() {
    val hardwareSpec = remember { collectHardwareSpec() }
    when (NavState.currentScreen.value) {
        "title" -> TitleFragment(onEnter = { NavState.currentScreen.value = "update" })
        "update" -> UpdateFragment(
            onSkip = { NavState.currentScreen.value = "login" },
            onContinue = { NavState.currentScreen.value = "title" }
        )
        "login" -> LoginFragment(
            onRegister = { NavState.currentScreen.value = "register" },
            onBack = { NavState.currentScreen.value = "title" }
        )
        "register" -> RegisterFragment(
            onRegisterSuccess = { NavState.currentScreen.value = "login" },
            onBack = { NavState.currentScreen.value = "login" }
        )
        "profile" -> ProfileFragment(
            userId = NavState.currentUserId.value,
            hasMcResources = false,
            onLobby = { NavState.currentScreen.value = "hostLobby" },
            onMcAssets = { NavState.currentScreen.value = "mcManage" },
            onMail = { NavState.currentScreen.value = "mail" },
            onLogout = { NavState.currentScreen.value = "login" },
            onChangeProfile = { NavState.currentScreen.value = "changeProfile" },
            onWardrobe = { NavState.currentScreen.value = "wardrobe" },
            hwSpec = hardwareSpec
        )
        "hostLobby" -> HostLobbyFragment(
            onBack = { NavState.currentScreen.value = "profile" }
        )
        "mail" -> MailFragment(
            onBack = { NavState.currentScreen.value = "profile" }
        )
        "mcManage" -> McVersionManageFragment(
            onBack = { NavState.currentScreen.value = "profile" }
        )
        "wardrobe" -> WardrobeFragment(
            onBack = { NavState.currentScreen.value = "profile" }
        )
        "mojangSkin" -> MojangSkinFragment(
            onBack = { NavState.currentScreen.value = "wardrobe" }
        )
        "changeProfile" -> ChangeProfileFragment(
            initialName = NavState.currentUserId.value,
            onBack = { NavState.currentScreen.value = "profile" }
        )
    }
}
class RDI{

    @Composable
    fun start(content: @Composable () -> Unit) {
        content()
    }
}

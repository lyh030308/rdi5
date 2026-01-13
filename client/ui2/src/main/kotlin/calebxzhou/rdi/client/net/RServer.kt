package calebxzhou.rdi.client.net

import calebxzhou.rdi.client.SERVER_URL
import calebxzhou.rdi.common.model.Response
import calebxzhou.rdi.common.net.httpRequest
import calebxzhou.rdi.common.serdesJson
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonElement

data class RegisterResult(
    val response: Response<JsonElement>?,
    val httpStatus: Int
)

data class LoginResult(
    val response: Response<JsonElement>?,
    val httpStatus: Int,
    val jwt: String?
)

data class UpdateProfileResult(
    val response: Response<JsonElement>?,
    val httpStatus: Int
)

data class LoggedAccount(
    var id: String = "",
    var qq: String = ""
)

object RServer {
    val loggedAccount = LoggedAccount()

    suspend fun register(username: String, password: String, qq: String): RegisterResult {
        val response = httpRequest {
            url("$SERVER_URL/player/register")
            method = HttpMethod.Post
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("name", username)
                        append("pwd", password)
                        append("qq", qq)
                    }
                )
            )
        }
        val bodyText = response.bodyAsText()
        val parsed = runCatching {
            serdesJson.decodeFromString<Response<JsonElement>>(bodyText)
        }.getOrNull()
        return RegisterResult(parsed, response.status.value)
    }

    suspend fun login(username: String, password: String): LoginResult {
        val response = httpRequest {
            url("$SERVER_URL/player/login")
            method = HttpMethod.Post
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("usr", username)
                        append("pwd", password)
                    }
                )
            )
        }
        val jwt = response.headers["jwt"]
        return LoginResult(response.body<Response<JsonElement>>(), response.status.value, jwt)
    }

    suspend fun updateProfile(name: String?, pwd: String?, jwt: String?): UpdateProfileResult {
        val response = httpRequest {
            url("$SERVER_URL/player/profile")
            method = HttpMethod.Put
            if (!jwt.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $jwt")
            }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        if (!name.isNullOrBlank()) {
                            append("name", name)
                        }
                        if (!pwd.isNullOrBlank()) {
                            append("pwd", pwd)
                        }
                    }
                )
            )
        }
        val bodyText = response.bodyAsText()
        val parsed = runCatching {
            serdesJson.decodeFromString<Response<JsonElement>>(bodyText)
        }.getOrNull()
        return UpdateProfileResult(parsed, response.status.value)
    }
}

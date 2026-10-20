package com.thegreatyamori.saldos.data.cnt

import com.thegreatyamori.saldos.domain.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

sealed class CntException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidCredentials : CntException("Credenciales inválidas")
    class Upstream(val status: Int, val path: String) : CntException("CNT $path ($status)")
    class Network(cause: Throwable) : CntException("Sin conexión: ${cause.message}", cause)
    class Malformed(cause: Throwable) : CntException("Respuesta inesperada de CNT", cause)
}

class CntClient(
    baseUrl: String = DEFAULT_BASE_URL,
    private val origin: String = DEFAULT_ORIGIN,
    private val cookieJar: CookieJar,
    private val client: OkHttpClient = defaultClient(cookieJar)
) {
    private val base: HttpUrl = baseUrl.toHttpUrl()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    suspend fun fetchBalances(
        identifier: String,
        password: String,
        preferredPhoneValue: String? = null,
        serviceTypeHint: String? = null
    ): Snapshot = withContext(Dispatchers.IO) {
        try {
            val csrf = fetchCsrfToken()
            val refreshedCsrf = login(identifier, password, csrf)
            val user = fetchUser(refreshedCsrf)
            val typeUser = user.typeUser ?: "empresa"
            val serviceType = user.serviceType ?: serviceTypeHint ?: "movil"
            val rawIdentifier = user.identifier ?: identifier
            val payloadId = if (typeUser == "empresa" || serviceType == "movil") {
                rawIdentifier.drop(1)
            } else {
                rawIdentifier
            }
            val raw = fetchRawBalances(payloadId, typeUser, serviceType, refreshedCsrf)
            CntMapper.toSnapshot(raw, serviceType, preferredPhoneValue)
                ?: throw CntException.Malformed(IllegalStateException("CNT returned no lines"))
        } catch (error: CntException) {
            throw error
        } catch (error: IOException) {
            throw CntException.Network(error)
        } catch (error: Exception) {
            throw CntException.Malformed(error)
        }
    }

    private fun fetchCsrfToken(): String {
        val request = requestBuilder(path("sanctum/csrf-cookie"), csrf = "").get().build()
        val response = execute(request, "sanctum/csrf-cookie")
        if (response.status >= 500) throw CntException.Upstream(response.status, "sanctum/csrf-cookie")
        return readCookie("XSRF-TOKEN")
            ?: throw CntException.Malformed(IllegalStateException("missing XSRF-TOKEN cookie"))
    }

    private fun login(identifier: String, password: String, csrf: String): String {
        val body = json.encodeToString(
            LoginRequest(identifier = identifier, password = CntCrypto.encrypt(password))
        )
        val request = requestBuilder(path("api/login"), csrf)
            .post(body.toRequestBody(JSON))
            .build()
        val response = execute(request, "api/login")
        if (response.status >= 400) throw CntException.InvalidCredentials()
        return readCookie("XSRF-TOKEN") ?: csrf
    }

    private fun fetchUser(csrf: String): CntUser {
        val request = requestBuilder(path("api/user"), csrf).get().build()
        val response = execute(request, "api/user")
        if (response.status >= 400 || response.body.isBlank()) {
            throw CntException.Upstream(response.status, "api/user")
        }
        return try {
            json.decodeFromString(CntUser.serializer(), response.body)
        } catch (error: Exception) {
            throw CntException.Malformed(error)
        }
    }

    private fun fetchRawBalances(
        identifier: String,
        typeUser: String,
        serviceType: String,
        csrf: String
    ): List<RawGroup> {
        val body = json.encodeToString(
            BalancesRequest(
                identifier = CntCrypto.encrypt(identifier),
                typeUser = typeUser,
                serviceType = serviceType
            )
        )
        val request = requestBuilder(path("api/balances_consumed/"), csrf)
            .post(body.toRequestBody(JSON))
            .build()
        val response = execute(request, "api/balances_consumed/")
        if (response.status >= 400) {
            throw CntException.Upstream(response.status, "api/balances_consumed/")
        }
        return try {
            json.decodeFromString(ListSerializer(RawGroup.serializer()), response.body)
        } catch (error: Exception) {
            throw CntException.Malformed(error)
        }
    }

    private fun readCookie(name: String): String? {
        val cookie = cookieJar.loadForRequest(base).firstOrNull { it.name == name } ?: return null
        return URLDecoder.decode(cookie.value, "UTF-8")
    }

    private fun execute(request: Request, path: String): HttpResult {
        val response = try {
            client.newCall(request).execute()
        } catch (error: IOException) {
            throw CntException.Network(error)
        }
        response.use { result ->
            return HttpResult(result.code, result.body?.string().orEmpty())
        }
    }

    private fun path(relative: String): HttpUrl =
        base.newBuilder().addPathSegments(relative).build()

    private fun requestBuilder(url: HttpUrl, csrf: String): Request.Builder = Request.Builder()
        .url(url)
        .header("Accept", "application/json, text/plain, */*")
        .header("X-Requested-With", "XMLHttpRequest")
        .header("Origin", origin)
        .header("Referer", "$origin/")
        .apply { if (csrf.isNotEmpty()) header("X-XSRF-TOKEN", csrf) }

    private data class HttpResult(val status: Int, val body: String)

    companion object {
        private const val DEFAULT_BASE_URL = "https://apiapp.cnt.gob.ec"
        private const val DEFAULT_ORIGIN = "https://app.cnt.gob.ec"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(cookieJar: CookieJar): OkHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

package com.thegreatyamori.saldos.data.cnt

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.File
import java.util.logging.Logger

/**
 * OkHttp [CookieJar] with optional on-disk persistence so the Sanctum session
 * survives process death.
 */
class PersistentCookieJar(
    private val file: File?,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : CookieJar {

    private val cookies = linkedMapOf<String, Cookie>()
    private val logger = Logger.getLogger(PersistentCookieJar::class.java.name)

    init {
        load()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this) {
            cookies.forEach { cookie ->
                this.cookies[key(cookie.domain, cookie.path, cookie.name)] = cookie
            }
            persist()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(this) {
            val now = System.currentTimeMillis()
            val expired = cookies.values.filter { it.expiresAt <= now }
            expired.forEach { cookies.remove(key(it.domain, it.path, it.name)) }
            if (expired.isNotEmpty()) persist()
            return cookies.values.filter { it.matches(url) }
        }
    }

    fun find(url: HttpUrl, name: String): Cookie? = synchronized(this) {
        loadForRequest(url).firstOrNull { it.name == name }
    }

    fun clear() = synchronized(this) {
        cookies.clear()
        file?.delete()
    }

    private fun key(domain: String, path: String, name: String): String = "$domain|$path|$name"

    private fun persist() {
        val target = file ?: return
        try {
            target.parentFile?.mkdirs()
            val payload = json.encodeToString(
                SERIALIZER,
                cookies.values.map { it.toDto() }
            )
            val temp = File(target.parentFile, "${target.name}.tmp")
            temp.writeText(payload)
            if (!temp.renameTo(target)) {
                target.writeText(payload)
                temp.delete()
            }
        } catch (error: Exception) {
            logger.warning("cookie persistence failed: ${error.message}")
        }
    }

    private fun load() {
        val target = file ?: return
        if (!target.exists()) return
        try {
            val list = json.decodeFromString(SERIALIZER, target.readText())
            list.forEach { dto ->
                val cookie = dto.toCookie() ?: return@forEach
                cookies[key(cookie.domain, cookie.path, cookie.name)] = cookie
            }
        } catch (error: Exception) {
            logger.warning("cookie cache unreadable: ${error.message}")
        }
    }

    @Serializable
    private data class CookieDto(
        val name: String,
        val value: String,
        val domain: String,
        val path: String,
        val expiresAt: Long,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean
    )

    private fun Cookie.toDto() = CookieDto(
        name = name,
        value = value,
        domain = domain,
        path = path,
        expiresAt = expiresAt,
        secure = secure,
        httpOnly = httpOnly,
        hostOnly = hostOnly
    )

    private fun CookieDto.toCookie(): Cookie? = try {
        Cookie.Builder()
            .name(name)
            .value(value)
            .path(path)
            .expiresAt(expiresAt)
            .apply {
                if (hostOnly) hostOnlyDomain(domain) else domain(domain)
                if (secure) secure()
                if (httpOnly) httpOnly()
            }
            .build()
    } catch (_: Exception) {
        null
    }

    private companion object {
        val SERIALIZER = ListSerializer(CookieDto.serializer())
    }
}

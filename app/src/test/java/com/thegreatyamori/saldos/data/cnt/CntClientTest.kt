package com.thegreatyamori.saldos.data.cnt

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CntClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `runs the cookie login flow and maps balances`() {
        server.dispatcher = happyPath()
        val client = client()

        runBlocking {
            val snapshot = client.fetchBalances("0983592938", "secret")

            assertEquals("0983592938", snapshot.header.phone)
            assertEquals("1.44", snapshot.hero?.value)
            assertEquals(4, snapshot.items.size)
        }

        val paths = generateSequence { server.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS) }
            .map { it.path }
            .toList()
        assertEquals(
            listOf("/sanctum/csrf-cookie", "/api/login", "/api/user", "/api/balances_consumed/"),
            paths
        )
    }

    @Test
    fun `sends the xsrf header on authenticated calls`() {
        server.dispatcher = happyPath()
        val client = client()

        runBlocking { client.fetchBalances("0983592938", "secret") }

        server.takeRequest()
        server.takeRequest()
        val userRequest = server.takeRequest()
        assertEquals("abc=", userRequest.getHeader("X-XSRF-TOKEN"))
        assertEquals("https://app.cnt.gob.ec", userRequest.getHeader("Origin"))
        assertEquals("XMLHttpRequest", userRequest.getHeader("X-Requested-With"))
    }

    @Test
    fun `rejects invalid credentials`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/sanctum/csrf-cookie" -> MockResponse()
                    .setResponseCode(204)
                    .addHeader("Set-Cookie", "XSRF-TOKEN=abc%3D; Path=/")
                "/api/login" -> MockResponse().setResponseCode(401).setBody("""{"message":"nope"}""")
                else -> MockResponse().setResponseCode(404)
            }
        }

        assertThrows(CntException.InvalidCredentials::class.java) {
            runBlocking { client().fetchBalances("0983592938", "wrong") }
        }
    }

    private fun client(): CntClient = CntClient(
        baseUrl = server.url("/").toString().trimEnd('/'),
        cookieJar = PersistentCookieJar(file = null)
    )

    private fun happyPath(): Dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
            "/sanctum/csrf-cookie" -> MockResponse()
                .setResponseCode(204)
                .addHeader("Set-Cookie", "XSRF-TOKEN=abc%3D; Path=/")
            "/api/login" -> MockResponse().setResponseCode(200).setBody("""{"ok":true}""")
            "/api/user" -> MockResponse().setBody(
                """{"identifier":"0983592938","type_user":"movil","service_type":"movil"}"""
            )
            "/api/balances_consumed/" -> MockResponse().setBody(SAMPLE)
            else -> MockResponse().setResponseCode(404)
        }
    }

    private companion object {
        val SAMPLE = """
        [
          {
            "type": "movil",
            "data": [
              {
                "title": "PLAN PREPAGO CHIP PLUS",
                "phone": "983592938",
                "phone_value": "servicio-0",
                "info": [
                  { "title": "GIGAS", "data": { "total": "5.00", "consumed": "1.44", "meter": "GB" } },
                  { "title": "FACEBOOK", "data": { "total": "1024", "consumed": "1024", "meter": "MB" } },
                  { "title": "SMS", "data": { "total": "50", "consumed": "48", "meter": "SMS" } },
                  { "title": "RECARGAS", "data": { "total": 0, "consumed": 0, "meter": "USD" } }
                ]
              }
            ]
          }
        ]
        """.trimIndent()
    }
}

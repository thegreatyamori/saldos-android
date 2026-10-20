package com.thegreatyamori.saldos.data.cnt

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CntMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val sample: List<RawGroup> = json.decodeFromString(
        ListSerializer(RawGroup.serializer()),
        SAMPLE
    )

    @Test
    fun `maps the first line and its hero using consumed as remaining`() {
        val snapshot = CntMapper.toSnapshot(sample, "movil")!!
        assertEquals("0983592938", snapshot.header.phone)
        assertEquals("GIGAS", snapshot.hero?.label)
        assertEquals("1.44", snapshot.hero?.value)
        assertEquals("GB", snapshot.hero?.unit)
        assertEquals(0.712f, snapshot.hero?.ratio ?: 0f, 0.001f)
    }

    @Test
    fun `filters voice items`() {
        val labels = CntMapper.toSnapshot(sample, "movil")!!.items.map { it.label }
        assertFalse(labels.contains("SEGUNDOS_OFFNET"))
        assertFalse(labels.contains("SEGUNDOS_ONNET"))
    }

    @Test
    fun `assigns family colors and no ring for USD`() {
        val items = CntMapper.toSnapshot(sample, "movil")!!.items
        val facebook = items.first { it.label == "FACEBOOK" }
        val recargas = items.first { it.label == "RECARGAS" }
        assertEquals("1024", facebook.value)
        assertEquals("#1877F2", facebook.color)
        assertFalse(recargas.ring)
    }

    @Test
    fun `marks unlimited quotas`() {
        val unlimited = CntMapper.toSnapshot(
            listOf(
                RawGroup(
                    type = "movil",
                    data = listOf(
                        RawLine(
                            title = "P",
                            phone = "983592938",
                            phoneValue = "s0",
                            info = listOf(
                                RawBalanceItem(
                                    title = "GIGAS",
                                    data = RawItemData(
                                        total = kotlinx.serialization.json.JsonPrimitive("-1"),
                                        consumed = kotlinx.serialization.json.JsonPrimitive("-1"),
                                        meter = "GB"
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            "movil"
        )!!
        assertEquals("∞", unlimited.hero?.value)
        assertFalse(unlimited.hero?.ring ?: true)
    }

    @Test
    fun `returns null when there are no lines`() {
        assertNull(CntMapper.toSnapshot(listOf(RawGroup(type = "movil", data = emptyList())), "movil"))
    }

    @Test
    fun `lists lines`() {
        val lines = CntMapper.toLines(sample, "movil")
        assertEquals(1, lines.size)
        assertEquals("983592938", lines[0].phone)
        assertEquals("servicio-0", lines[0].phoneValue)
        assertEquals("PLAN PREPAGO CHIP PLUS", lines[0].plan)
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
                  { "title": "SEGUNDOS_OFFNET", "data": { "total": "50", "consumed": "37.65", "meter": "Minutos" } },
                  { "title": "SEGUNDOS_ONNET", "data": { "total": "-1", "consumed": "-1", "meter": "Minutos" } },
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

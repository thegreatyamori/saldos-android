package com.thegreatyamori.saldos.data.cnt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RawItemData(
    val total: JsonElement? = null,
    val consumed: JsonElement? = null,
    val meter: String? = null
)

@Serializable
data class RawBalanceItem(
    val title: String? = null,
    val description: String? = null,
    val data: RawItemData? = null
)

@Serializable
data class RawLine(
    val title: String? = null,
    val phone: String? = null,
    @SerialName("phone_value") val phoneValue: String? = null,
    val info: List<RawBalanceItem> = emptyList()
)

@Serializable
data class RawGroup(
    val type: String? = null,
    val data: List<RawLine> = emptyList()
)

@Serializable
data class CntUser(
    val identifier: String? = null,
    @SerialName("type_user") val typeUser: String? = null,
    @SerialName("service_type") val serviceType: String? = null
)

@Serializable
internal data class LoginRequest(
    val identifier: String,
    val password: String
)

@Serializable
internal data class BalancesRequest(
    val identifier: String,
    @SerialName("type_user") val typeUser: String,
    @SerialName("service_type") val serviceType: String
)

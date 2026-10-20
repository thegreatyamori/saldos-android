package com.thegreatyamori.saldos.data.settings

enum class RefreshPolicy {
    DATA_ONLY,
    ALWAYS,
    MANUAL
}

fun RefreshPolicy.allowsAutomaticRefresh(onWifi: Boolean): Boolean = when (this) {
    RefreshPolicy.DATA_ONLY -> !onWifi
    RefreshPolicy.ALWAYS -> true
    RefreshPolicy.MANUAL -> false
}

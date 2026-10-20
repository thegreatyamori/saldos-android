package com.thegreatyamori.saldos.data.creds

data class Credentials(
    val identifier: String,
    val password: String
)

interface CredentialStore {
    fun load(): Credentials?
    fun save(credentials: Credentials)
    fun clear()
}

package com.thegreatyamori.saldos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thegreatyamori.saldos.data.cnt.CntException
import com.thegreatyamori.saldos.data.settings.RefreshPolicy
import com.thegreatyamori.saldos.domain.BalancesRepository
import com.thegreatyamori.saldos.domain.RefreshResult
import com.thegreatyamori.saldos.domain.Snapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaldosUiState(
    val credentialsPresent: Boolean = false,
    val snapshot: Snapshot? = null,
    val refreshing: Boolean = false,
    val error: UiError? = null,
    val refreshPolicy: RefreshPolicy = RefreshPolicy.DATA_ONLY,
    val accountIdentifier: String? = null
)

sealed interface UiError {
    data object MissingFields : UiError
    data object InvalidCredentials : UiError
    data object Network : UiError
    data object Upstream : UiError
    data object Malformed : UiError
}

fun UiError.message(): String = when (this) {
    UiError.MissingFields -> "Completá cédula y contraseña"
    UiError.InvalidCredentials -> "Credenciales inválidas"
    UiError.Network -> "Sin conexión"
    UiError.Upstream -> "CNT no responde"
    UiError.Malformed -> "Respuesta inesperada de CNT"
}

class SaldosViewModel(private val repository: BalancesRepository) : ViewModel() {

    private val _state = MutableStateFlow(
        SaldosUiState(
            credentialsPresent = repository.hasCredentials(),
            snapshot = repository.cached(),
            refreshPolicy = repository.refreshPolicy(),
            accountIdentifier = repository.identifier()
        )
    )
    val state: StateFlow<SaldosUiState> = _state.asStateFlow()

    fun login(identifier: String, password: String) {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty() || password.isEmpty()) {
            _state.update { it.copy(error = UiError.MissingFields) }
            return
        }
        fetch(keepCredentialsOnFailure = false) { repository.login(trimmed, password) }
    }

    fun refresh() {
        if (!_state.value.credentialsPresent) return
        fetch(keepCredentialsOnFailure = true) { repository.refresh() }
    }

    fun selectLine(phoneValue: String) {
        repository.selectLine(phoneValue)
        refresh()
    }

    fun setRefreshPolicy(policy: RefreshPolicy) {
        repository.setRefreshPolicy(policy)
        _state.update { it.copy(refreshPolicy = policy) }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.value = SaldosUiState(refreshPolicy = repository.refreshPolicy())
        }
    }

    private fun fetch(
        keepCredentialsOnFailure: Boolean,
        action: suspend () -> RefreshResult
    ) {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            when (val result = action()) {
                is RefreshResult.Success -> _state.update {
                    it.copy(
                        credentialsPresent = true,
                        snapshot = result.snapshot,
                        refreshing = false,
                        error = null,
                        accountIdentifier = repository.identifier() ?: it.accountIdentifier
                    )
                }
                is RefreshResult.Failure -> _state.update {
                    it.copy(
                        credentialsPresent = keepCredentialsOnFailure || it.credentialsPresent,
                        snapshot = result.cached ?: it.snapshot,
                        refreshing = false,
                        error = result.error.toUiError()
                    )
                }
            }
        }
    }

    private fun Throwable.toUiError(): UiError = when (this) {
        is CntException.InvalidCredentials -> UiError.InvalidCredentials
        is CntException.Network -> UiError.Network
        is CntException.Upstream -> UiError.Upstream
        is CntException.Malformed -> UiError.Malformed
        else -> UiError.Malformed
    }

    companion object {
        fun factory(repository: BalancesRepository) = viewModelFactory {
            initializer { SaldosViewModel(repository) }
        }
    }
}

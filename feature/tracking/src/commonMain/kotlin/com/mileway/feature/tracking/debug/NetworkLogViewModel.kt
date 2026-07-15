package com.mileway.feature.tracking.debug

import androidx.lifecycle.viewModelScope
import com.mileway.core.network.netlog.NetworkLogEntry
import com.mileway.core.network.netlog.NetworkLogStore
import com.siddharth.kmp.mvi.BaseViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class NetworkLogUiState(
    val entries: List<NetworkLogEntry> = emptyList(),
    val selected: NetworkLogEntry? = null,
    // API tester form fields.
    val testerMethod: String = "GET",
    val testerUrl: String = "",
    val testerBody: String = "",
    val testerResult: String? = null,
    val testerRunning: Boolean = false,
)

sealed interface NetworkLogAction {
    data class SelectEntry(val entry: NetworkLogEntry?) : NetworkLogAction

    data object ClearLog : NetworkLogAction

    data class TesterMethodChanged(val method: String) : NetworkLogAction

    data class TesterUrlChanged(val url: String) : NetworkLogAction

    data class TesterBodyChanged(val body: String) : NetworkLogAction

    data object TesterSend : NetworkLogAction
}

/**
 * Backs the debug NetworkLogScreen: the entry list/detail comes straight from [NetworkLogStore],
 * plus a minimal API tester that sends one request through [httpClient] if the host app provides
 * one (Koin `getOrNull()` — this app is offline/:stub today, so no client may be wired yet).
 */
class NetworkLogViewModel(
    private val store: NetworkLogStore,
    private val httpClient: HttpClient?,
) : BaseViewModel<NetworkLogUiState, Unit, NetworkLogAction>(NetworkLogUiState()) {
    init {
        store.entries.onEach { list -> setState { copy(entries = list) } }.launchIn(viewModelScope)
    }

    override fun onAction(action: NetworkLogAction) {
        when (action) {
            is NetworkLogAction.SelectEntry -> setState { copy(selected = action.entry) }
            is NetworkLogAction.ClearLog -> store.clear()
            is NetworkLogAction.TesterMethodChanged -> setState { copy(testerMethod = action.method) }
            is NetworkLogAction.TesterUrlChanged -> setState { copy(testerUrl = action.url) }
            is NetworkLogAction.TesterBodyChanged -> setState { copy(testerBody = action.body) }
            is NetworkLogAction.TesterSend -> send()
        }
    }

    private fun send() {
        val client = httpClient
        if (client == null) {
            val message = "No HttpClient wired in this build — install NetworkLogPlugin on one to enable the tester."
            setState { copy(testerResult = message) }
            return
        }
        val method = currentState.testerMethod
        val url = currentState.testerUrl
        val body = currentState.testerBody
        setState { copy(testerRunning = true, testerResult = null) }
        viewModelScope.launch {
            val result =
                runCatching {
                    client.request {
                        this.method = HttpMethod.parse(method)
                        url(url)
                        if (body.isNotBlank()) setBody(body)
                    }
                }.fold(
                    onSuccess = { "HTTP ${it.status.value}" },
                    onFailure = { "Error: ${it.message}" },
                )
            setState { copy(testerRunning = false, testerResult = result) }
        }
    }
}

package dev.xsam.splitwise.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.xsam.splitwise.data.ApiClient
import dev.xsam.splitwise.data.AddMemberRequest
import dev.xsam.splitwise.data.AuthResponse
import dev.xsam.splitwise.data.CreateExpenseRequest
import dev.xsam.splitwise.data.CreateGroupRequest
import dev.xsam.splitwise.data.ErrorResponse
import dev.xsam.splitwise.data.GroupDetailDto
import dev.xsam.splitwise.data.GroupSummaryDto
import dev.xsam.splitwise.data.LoginRequest
import dev.xsam.splitwise.data.SignupRequest
import dev.xsam.splitwise.data.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

sealed interface AppScreen {
    data object Auth : AppScreen
    data object Groups : AppScreen
    data class Detail(val groupId: String) : AppScreen
}

data class AppUiState(
    val token: String? = null,
    val username: String? = null,
    val screen: AppScreen = AppScreen.Auth,
    val groups: List<GroupSummaryDto> = emptyList(),
    val groupDetail: GroupDetailDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AppViewModel(private val tokenStore: TokenStore) : ViewModel() {
    private val api = ApiClient.api
    private val errorJson = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state

    init {
        val token = tokenStore.token
        if (token != null) {
            _state.value = AppUiState(
                token = token,
                username = tokenStore.username,
                screen = AppScreen.Groups,
            )
            loadGroups()
        }
    }

    fun signup(username: String, password: String) = authenticate { api.signup(SignupRequest(username, password)) }

    fun login(username: String, password: String) = authenticate { api.login(LoginRequest(username, password)) }

    fun logout() {
        tokenStore.clear()
        _state.value = AppUiState()
    }

    fun loadGroups() {
        val token = _state.value.token ?: return
        startLoading()
        viewModelScope.launch {
            try {
                val groups = api.groups(bearer(token)).groups
                _state.value = _state.value.copy(groups = groups, isLoading = false, error = null)
            } catch (error: Throwable) {
                fail(error)
            }
        }
    }

    fun openGroup(groupId: String) {
        val token = _state.value.token ?: return
        _state.value = _state.value.copy(screen = AppScreen.Detail(groupId), groupDetail = null)
        startLoading()
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(groupDetail = api.group(bearer(token), groupId).group, isLoading = false)
            } catch (error: Throwable) {
                fail(error)
            }
        }
    }

    fun backToGroups() {
        _state.value = _state.value.copy(screen = AppScreen.Groups, groupDetail = null, error = null)
    }

    fun createGroup(name: String, memberUsernames: List<String>) {
        val token = _state.value.token ?: return
        startLoading()
        viewModelScope.launch {
            try {
                val detail = api.createGroup(bearer(token), CreateGroupRequest(name, memberUsernames)).group
                applyDetail(detail)
            } catch (error: Throwable) {
                fail(error)
            }
        }
    }

    fun addMember(groupId: String, username: String) {
        val token = _state.value.token ?: return
        startLoading()
        viewModelScope.launch {
            try {
                val detail = api.addMember(bearer(token), groupId, AddMemberRequest(username)).group
                applyDetail(detail)
            } catch (error: Throwable) {
                fail(error)
            }
        }
    }

    fun addExpense(groupId: String, request: CreateExpenseRequest) {
        val token = _state.value.token ?: return
        startLoading()
        viewModelScope.launch {
            try {
                val detail = api.addExpense(bearer(token), groupId, request).group
                applyDetail(detail)
            } catch (error: Throwable) {
                fail(error)
            }
        }
    }

    fun deleteExpense(groupId: String, expenseId: String) {
        val token = _state.value.token ?: return
        startLoading()
        viewModelScope.launch {
            try {
                val detail = api.deleteExpense(bearer(token), groupId, expenseId).group
                applyDetail(detail)
            } catch (error: Throwable) {
                fail(error)
            }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun authenticate(login: suspend () -> AuthResponse) {
        startLoading()
        viewModelScope.launch {
            try {
                val response = login()
                tokenStore.save(response.token, response.user.username)
                _state.value = AppUiState(
                    token = response.token,
                    username = response.user.username,
                    screen = AppScreen.Groups,
                )
                loadGroups()
            } catch (error: Throwable) {
                fail(error)
            }
        }
    }

    private suspend fun applyDetail(detail: GroupDetailDto) {
        val token = _state.value.token ?: return
        val groups = api.groups(bearer(token)).groups
        _state.value = _state.value.copy(
            groups = groups,
            groupDetail = detail,
            screen = AppScreen.Detail(detail.id),
            isLoading = false,
            error = null,
        )
    }

    private fun startLoading() {
        _state.value = _state.value.copy(isLoading = true, error = null)
    }

    private fun fail(error: Throwable) {
        _state.value = _state.value.copy(isLoading = false, error = readableError(error))
    }

    private fun readableError(error: Throwable): String {
        if (error is IOException) return "Could not reach the server. Check that the API is running."
        if (error is HttpException) {
            val body = error.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                try {
                    return errorJson.decodeFromString<ErrorResponse>(body).error
                } catch (_: Exception) {
                    // Fall through to the generic HTTP message.
                }
            }
            return "Request failed (${error.code()})"
        }
        return error.message ?: "Something went wrong"
    }

    private fun bearer(token: String) = "Bearer $token"

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                return AppViewModel(TokenStore(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: $modelClass")
        }
    }
}

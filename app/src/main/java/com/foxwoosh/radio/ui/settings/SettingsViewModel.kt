package com.foxwoosh.radio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxwoosh.radio.R
import com.foxwoosh.radio.domain.interactors.settings.ISettingsInteractor
import com.foxwoosh.radio.domain.interactors.settings.exceptions.AuthDataException
import com.foxwoosh.radio.ui.settings.models.AuthFieldsErrorState
import com.foxwoosh.radio.ui.settings.models.AuthFieldsState
import com.foxwoosh.radio.ui.settings.models.SettingsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val interactor: ISettingsInteractor
) : ViewModel() {
    private val mutableEvents = MutableSharedFlow<SettingsEvent>()
    val events = mutableEvents.asSharedFlow()

    private val mutableAuthFieldsErrorState = MutableStateFlow(AuthFieldsErrorState())
    val authFieldsErrorState = mutableAuthFieldsErrorState.asStateFlow()

    private val mutableAuthFieldsState = MutableStateFlow(AuthFieldsState())
    val authFieldsState = mutableAuthFieldsState.asStateFlow()

    val userState = interactor.user.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = null
    )

    fun register() {
        viewModelScope.launch {
            try {
                val fields = authFieldsState.value

                mutableAuthFieldsErrorState.update { it.toNone() }
                interactor.register(fields.login, fields.password, fields.name, fields.email)

                mutableAuthFieldsState.update { it.clear() }
            } catch (e: Exception) {
                when (e) {
                    is AuthDataException.Login -> {
                        mutableAuthFieldsErrorState.update { it.toLogin() }
                        mutableEvents.emit(
                            SettingsEvent.Error(R.string.settings_auth_error_login_verification)
                        )
                    }
                    is AuthDataException.Password -> {
                        mutableAuthFieldsErrorState.update { it.toPassword() }
                        mutableEvents.emit(
                            SettingsEvent.Error(R.string.settings_auth_error_password_verification)
                        )
                    }
                    is AuthDataException.Name -> {
                        mutableAuthFieldsErrorState.update { it.toName() }
                        mutableEvents.emit(
                            SettingsEvent.Error(R.string.settings_auth_error_name_verification)
                        )
                    }
                    is AuthDataException.Email -> {
                        mutableAuthFieldsErrorState.update { it.toEmail() }
                        mutableEvents.emit(
                            SettingsEvent.Error(R.string.settings_auth_error_email_verification)
                        )
                    }
                    is HttpException -> {
                        when (e.code()) {
                            409 -> {
                                mutableAuthFieldsErrorState.update { it.toLogin() }
                                mutableEvents.emit(
                                    SettingsEvent.Error(R.string.settings_auth_error_login_conflict)
                                )
                            }
                            else -> mutableEvents.emit(SettingsEvent.Error(R.string.common_error))
                        }
                    }
                }
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            try {
                val fields = authFieldsState.value
                interactor.login(fields.login, fields.password)

                mutableAuthFieldsState.update { it.clear() }
            } catch (e: Exception) {
                when (e) {
                    is AuthDataException.Login -> {
                        mutableAuthFieldsErrorState.update { it.toLogin() }
                        mutableEvents.emit(
                            SettingsEvent.Error(R.string.settings_auth_error_login_verification)
                        )
                    }
                    is AuthDataException.Password -> {
                        mutableAuthFieldsErrorState.update { it.toPassword() }
                        mutableEvents.emit(
                            SettingsEvent.Error(R.string.settings_auth_error_password_verification)
                        )
                    }
                    is HttpException -> {
                        when (e.code()) {
                            403 -> mutableEvents.emit(
                                SettingsEvent.Error(R.string.settings_auth_error_wrong_credentials)
                            )
                            else -> mutableEvents.emit(SettingsEvent.Error(R.string.common_error))
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            interactor.logout()
        }
    }

    fun setAuthField(type: AuthFieldsState.Type, data: String) {
        mutableAuthFieldsState.update {
            when (type) {
                AuthFieldsState.Type.LOGIN -> it.copy(login = data)
                AuthFieldsState.Type.PASSWORD -> it.copy(password = data)
                AuthFieldsState.Type.NAME -> it.copy(name = data)
                AuthFieldsState.Type.EMAIL -> it.copy(email = data)
            }
        }
    }
}
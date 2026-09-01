package com.cr.tunnel.ui.base

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cr.tunnel.AngApplication
import com.cr.tunnel.extension.toast
import com.cr.tunnel.extension.toastError
import com.cr.tunnel.extension.toastSuccess
import com.cr.tunnel.handler.AppLocaleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {

    protected val app: AngApplication by lazy {
        application as AngApplication
    }

    protected val localizedContext: Context
        get() = AppLocaleManager.localizedContext(app)

    @Suppress("PropertyName")
    protected val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @Suppress("PropertyName")
    protected val _viewModelEvent = Channel<ViewModelEvent>()
    val viewModelEvent = _viewModelEvent.receiveAsFlow()

    fun toast(resId: Int) {
        localizedContext.toast(resId)
    }

    fun toast(message: String) {
        app.toast(message)
    }

    fun toastSuccess(resId: Int) {
        localizedContext.toastSuccess(resId)
    }

    fun toastSuccess(message: String) {
        app.toastSuccess(message)
    }

    fun toastError(resId: Int) {
        localizedContext.toastError(resId)
    }

    fun toastError(message: String) {
        app.toastError(message)
    }

    fun getString(resId: Int): String {
        return localizedContext.getString(resId)
    }

    fun getString(resId: Int, vararg formatArgs: Any?): String {
        return localizedContext.getString(resId, *formatArgs)
    }

    fun finishActivity() {
        viewModelScope.launch {
            _viewModelEvent.send(BaseViewModelEvent.FinishActivity)
        }
    }

    protected fun launchLoading(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                block()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

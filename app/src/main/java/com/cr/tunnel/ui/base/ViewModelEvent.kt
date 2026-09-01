package com.cr.tunnel.ui.base

interface ViewModelEvent

interface BaseViewModelEvent : ViewModelEvent {
    object FinishActivity : BaseViewModelEvent
}

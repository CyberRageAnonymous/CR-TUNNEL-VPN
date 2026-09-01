package com.cr.tunnel.enums

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.cr.tunnel.R

enum class PermissionType {
    
    CAMERA {
        override fun getPermission(): String = Manifest.permission.CAMERA
    },

    POST_NOTIFICATIONS {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun getPermission(): String = Manifest.permission.POST_NOTIFICATIONS
    },

    ACCESS_LOCAL_NETWORK {
        @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
        override fun getPermission(): String = Manifest.permission.ACCESS_LOCAL_NETWORK
    };

    abstract fun getPermission(): String

    @StringRes
    fun getLabelRes(): Int {
        return when (this) {
            CAMERA -> R.string.permission_camera
            POST_NOTIFICATIONS -> R.string.permission_notification
            ACCESS_LOCAL_NETWORK -> R.string.permission_local_network
        }
    }
}
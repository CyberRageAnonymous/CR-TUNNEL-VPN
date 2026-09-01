package com.cr.tunnel.helper

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.cr.tunnel.R
import com.cr.tunnel.enums.PermissionType
import com.cr.tunnel.extension.toast

class PermissionHelper(private val activity: ComponentActivity) {
    private var permissionCallback: ((Boolean) -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            permissionCallback?.invoke(isGranted)
            permissionCallback = null
        }

    fun request(permissionType: PermissionType, onGranted: () -> Unit) {
        val permission = permissionType.getPermission()
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            permissionCallback = { isGranted ->
                if (isGranted) {
                    onGranted()
                } else {
                    val message = activity.getString(
                        R.string.toast_permission_denied_for,
                        activity.getString(permissionType.getLabelRes())
                    )
                    activity.toast(message)
                }
            }
            permissionLauncher.launch(permission)
        }
    }
}

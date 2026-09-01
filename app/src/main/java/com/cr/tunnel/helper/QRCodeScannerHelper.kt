package com.cr.tunnel.helper

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.cr.tunnel.ui.ScannerActivity

class QRCodeScannerHelper(private val activity: ComponentActivity) {
    private var scanCallback: ((String?) -> Unit)? = null

    private val scanLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanCallback?.invoke(scanResult)
            } else {
                scanCallback?.invoke(null)
            }
            scanCallback = null
        }

    fun launch(onResult: (String?) -> Unit) {
        scanCallback = onResult
        scanLauncher.launch(Intent(activity, ScannerActivity::class.java))
    }
}

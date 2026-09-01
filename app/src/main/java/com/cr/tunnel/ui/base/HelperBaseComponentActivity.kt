package com.cr.tunnel.ui.base

import android.net.Uri
import android.os.Bundle
import com.cr.tunnel.enums.PermissionType
import com.cr.tunnel.helper.FileChooserHelper
import com.cr.tunnel.helper.PermissionHelper
import com.cr.tunnel.helper.QRCodeScannerHelper

abstract class HelperBaseComponentActivity : BaseComponentActivity() {

    private lateinit var fileChooser: FileChooserHelper
    private lateinit var permissionRequester: PermissionHelper
    private lateinit var qrCodeScanner: QRCodeScannerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileChooser = FileChooserHelper(this)
        permissionRequester = PermissionHelper(this)
        qrCodeScanner = QRCodeScannerHelper(this)
    }

    protected fun checkAndRequestPermission(
        permissionType: PermissionType,
        onGranted: () -> Unit
    ) {
        permissionRequester.request(permissionType, onGranted)
    }

    protected fun launchFileChooser(
        mimeType: String = "*/*",
        onResult: (Uri?) -> Unit
    ) {
        fileChooser.launch(mimeType, onResult)
    }

    protected fun launchCreateDocument(
        fileName: String,
        onResult: (Uri?) -> Unit
    ) {
        fileChooser.createDocument(fileName, onResult)
    }

    protected fun launchQRCodeScanner(
        onResult: (String?) -> Unit
    ) {
        qrCodeScanner.launch(onResult)
    }
}
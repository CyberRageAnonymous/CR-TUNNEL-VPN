package com.cr.tunnel.ui

internal object AppSelection {

    fun invert(currentSelection: Set<String>, packageNames: Collection<String>): Set<String> {
        return currentSelection.toMutableSet().apply {
            packageNames.forEach { packageName ->
                if (!add(packageName)) {
                    remove(packageName)
                }
            }
        }
    }

    fun fromProxyList(
        packageNames: Collection<String>,
        proxyAppList: String,
        bypassApps: Boolean,
        forceGoogleApps: Boolean
    ): Set<String> {
        return buildSet(packageNames.size) {
            packageNames.forEach { packageName ->
                val shouldProxy = shouldProxy(packageName, proxyAppList, forceGoogleApps)
                val shouldSelect = if (bypassApps) !shouldProxy else shouldProxy
                if (shouldSelect) {
                    add(packageName)
                }
            }
        }
    }

    private fun shouldProxy(packageName: String, proxyAppList: String, forceGoogleApps: Boolean): Boolean {
        if (forceGoogleApps) {
            if (packageName == "com.google.android.webview") return false
            if (packageName.startsWith("com.google")) return true
        }
        return proxyAppList.contains(packageName)
    }
}

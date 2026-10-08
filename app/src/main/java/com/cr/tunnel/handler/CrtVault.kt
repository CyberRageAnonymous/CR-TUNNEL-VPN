package com.cr.tunnel.handler

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CrtVault {

    private const val MAGIC = "CRT1"
    private const val SECRET = "CR-TUNNEL-CRT-CyberRage-2026"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    private val key: SecretKeySpec by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(SECRET.toByteArray(Charsets.UTF_8))
        SecretKeySpec(digest, "AES")
    }

    fun isCrt(text: String?): Boolean = text?.trimStart()?.startsWith(MAGIC) == true

    fun encrypt(plain: String): String {
        val iv = ByteArray(IV_BYTES)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val sealed = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return MAGIC + Base64.encodeToString(iv + sealed, Base64.NO_WRAP)
    }

    fun decrypt(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trim()
        if (!trimmed.startsWith(MAGIC)) return null
        return runCatching {
            val data = Base64.decode(trimmed.removePrefix(MAGIC), Base64.NO_WRAP)
            require(data.size > IV_BYTES)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(GCM_TAG_BITS, data.copyOfRange(0, IV_BYTES))
            )
            String(cipher.doFinal(data.copyOfRange(IV_BYTES, data.size)), Charsets.UTF_8)
        }.getOrNull()
    }
}

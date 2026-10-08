package com.cr.tunnel.handler

import android.util.Base64
import com.cr.tunnel.AngApplication
import com.cr.tunnel.AppConfig
import com.cr.tunnel.util.LogUtil
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

object NpvtDecoder {

    private const val TABLES_ASSET = "npvt_tables.bin.gz"
    private const val MAGIC = "NPVTTBL1"
    private val PERM = intArrayOf(0, 5, 10, 15, 4, 9, 14, 3, 8, 13, 2, 7, 12, 1, 6, 11)
    private val SPLIT = Regex("\\s*,\\s*")
    private val WS = Regex("\\s+")

    private var tablesReady = false
    private var nr = 0
    private var xor = IntArray(0)
    private var tyboxes = IntArray(0)
    private var tboxesLast = IntArray(0)
    private var mbl = IntArray(0)

    fun decodeServers(content: String): String? {
        return try {
            val trimmed = content.trim()
            if (!trimmed.startsWith("NPVT1")) return null
            ensureTables()
            if (!tablesReady) return null
            val parts = trimmed.substring(5).trim().split(SPLIT)
            if (parts.size != 3) return null
            val version = String(decryptPart(parts[0]), Charsets.UTF_8).trim()
            if ((version.toIntOrNull() ?: Int.MAX_VALUE) > 1) return null
            val servers = String(decryptPart(parts[1]), Charsets.UTF_8).trim()
            if (!servers.startsWith("[")) return null
            servers
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to decode npvt config", e)
            null
        }
    }

    private fun decryptPart(part: String): ByteArray {
        val cleaned = part.replace(WS, "")
        val binary = decodeBase64(cleaned) ?: throw IllegalArgumentException("bad base64")
        if (binary.size < 32) throw IllegalArgumentException("truncated payload")
        return decrypt(binary)
    }

    private fun decodeBase64(text: String): ByteArray? {
        return runCatching { Base64.decode(text, Base64.NO_WRAP) }.getOrNull()
            ?: runCatching {
                Base64.decode(text, Base64.NO_WRAP or Base64.URL_SAFE)
            }.getOrNull()
    }

    private fun ensureTables() {
        if (tablesReady) return
        synchronized(this) {
            if (tablesReady) return
            val bytes = ByteArrayOutputStream().use { out ->
                AngApplication.application.assets.open(TABLES_ASSET).use { input ->
                    GZIPInputStream(input).use { gz -> gz.copyTo(out) }
                }
                out.toByteArray()
            }
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = ByteArray(8)
            bb.get(magic)
            if (String(magic, Charsets.US_ASCII) != MAGIC) return
            nr = bb.int
            xor = readBlock(bb)
            tyboxes = readBlock(bb)
            tboxesLast = readBlock(bb)
            mbl = readBlock(bb)
            tablesReady = true
        }
    }

    private fun readBlock(bb: ByteBuffer): IntArray {
        val n = bb.int
        val data = IntArray(n)
        for (i in 0 until n) data[i] = bb.int
        return data
    }

    private fun xorAt(r: Int, a: Int, b: Int, c: Int): Int = xor[((r * 96 + a) * 16 + b) * 16 + c]
    private fun tyAt(r: Int, b: Int, c: Int): Int = tyboxes[(r * 16 + b) * 256 + c]
    private fun tbAt(b: Int, c: Int): Int = tboxesLast[b * 256 + c]
    private fun mblAt(r: Int, b: Int, c: Int): Int = mbl[(r * 16 + b) * 256 + c]

    private fun shiftRows(b: IntArray) {
        val copy = b.copyOf()
        for (i in 0 until 16) b[i] = copy[PERM[i]]
    }

    private fun encryptBlock(input: IntArray): IntArray {
        val b = input.copyOf()
        val lastRound = nr - 1
        for (round in 0 until lastRound) {
            shiftRows(b)
            for (i11 in 0 until 4) {
                val i13 = i11 * 4
                val i14 = i13 + 1
                val i15 = i13 + 2
                val i16 = i13 + 3
                val iC = tyAt(round, i13, b[i13] and 255)
                val iC2 = tyAt(round, i14, b[i14] and 255)
                val iC3 = tyAt(round, i15, b[i15] and 255)
                val iC4 = tyAt(round, i16, b[i16] and 255)
                for (i17 in 0 until 4) {
                    val i18 = i11 * 24 + i17 * 6
                    val i19 = i17 * 8
                    val i20 = 28 - i19
                    val i22 = 24 - i19
                    val b10 = xorAt(round, i18, (iC ushr i20) and 15, (iC2 ushr i20) and 15)
                    val b11 = xorAt(round, i18 + 1, (iC3 ushr i20) and 15, (iC4 ushr i20) and 15)
                    val idx2 = xorAt(round, i18 + 2, (iC ushr i22) and 15, (iC2 ushr i22) and 15) and 255
                    val idx3 = xorAt(round, i18 + 3, (iC3 ushr i22) and 15, (iC4 ushr i22) and 15) and 255
                    val val5 = xorAt(round, i18 + 5, idx2, idx3)
                    val val4 = xorAt(round, i18 + 4, b10 and 255, b11 and 255)
                    b[i13 + i17] = (val5 or (val4 shl 4)) and 255
                }
                val iC5 = mblAt(round, i13, b[i13] and 255)
                val iC6 = mblAt(round, i14, b[i14] and 255)
                val iC7 = mblAt(round, i15, b[i15] and 255)
                val iC8 = mblAt(round, i16, b[i16] and 255)
                for (i27 in 0 until 4) {
                    val i28 = i11 * 24 + i27 * 6
                    val i29 = i27 * 8
                    val i30 = 28 - i29
                    val i31 = 24 - i29
                    val e0 = xorAt(round, i28, (iC5 ushr i30) and 15, (iC6 ushr i30) and 15) and 255
                    val e1 = xorAt(round, i28 + 1, (iC7 ushr i30) and 15, (iC8 ushr i30) and 15) and 255
                    val e2 = xorAt(round, i28 + 2, (iC5 ushr i31) and 15, (iC6 ushr i31) and 15) and 255
                    val e3 = xorAt(round, i28 + 3, (iC7 ushr i31) and 15, (iC8 ushr i31) and 15) and 255
                    val v4 = xorAt(round, i28 + 4, e0, e1) and 255
                    val v5 = xorAt(round, i28 + 5, e2, e3)
                    b[i13 + i27] = ((v4 shl 4) or v5) and 255
                }
            }
        }
        shiftRows(b)
        val out = IntArray(16)
        for (i in 0 until 16) out[i] = tbAt(i, b[i] and 255) and 255
        return out
    }

    private fun decrypt(input: ByteArray): ByteArray {
        val nonce = input.copyOfRange(0, 16)
        val ct = input.copyOfRange(16, input.size)
        val counter = nonce.copyOf()
        val out = ByteArray(ct.size)
        val ks = ByteArray(16)
        for (i in ct.indices) {
            if (i and 15 == 0) {
                val blk = IntArray(16)
                for (j in 0 until 16) blk[j] = counter[j].toInt() and 255
                val r = encryptBlock(blk)
                for (j in 0 until 16) ks[j] = r[j].toByte()
                var idx = 15
                while (idx > -1) {
                    val nb = (counter[idx].toInt() and 255) + 1
                    val signed = if (nb > 127) nb - 256 else nb
                    counter[idx] = nb.toByte()
                    idx = if (signed == 0) idx - 1 else -1
                }
            }
            out[i] = (ks[i and 15].toInt() xor ct[i].toInt()).toByte()
        }
        return out
    }
}

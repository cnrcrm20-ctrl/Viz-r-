package com.example.util

import android.util.Base64

object CryptoHelper {
    private const val SECRET_KEY = "Vizor_Secure_Optal_Key_2026"

    fun encrypt(raw: String): String {
        return try {
            val bytes = raw.toByteArray(Charsets.UTF_8)
            val keyBytes = SECRET_KEY.toByteArray(Charsets.UTF_8)
            val encryptedBytes = ByteArray(bytes.size)
            for (i in bytes.indices) {
                encryptedBytes[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            raw
        }
    }

    fun decrypt(encryptedBase64: String): String {
        return try {
            val bytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val keyBytes = SECRET_KEY.toByteArray(Charsets.UTF_8)
            val decryptedBytes = ByteArray(bytes.size)
            for (i in bytes.indices) {
                decryptedBytes[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedBase64
        }
    }
}

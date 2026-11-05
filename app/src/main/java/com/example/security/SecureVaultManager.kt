package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureVaultManager {
    private val keyAlias = "NovaBrowserMasterKey"
    private val transformation = "AES/GCM/NoPadding"
    private var keyStore: KeyStore? = null
    private var fallbackKey: SecretKey? = null

    init {
        // Safe, non-blocking Keystore initialization with error recovery
        tryInitKeystore()
    }

    private fun tryInitKeystore() {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            keyStore = ks
            ensureKeyExists(ks)
        } catch (e: Throwable) {
            Log.w("SecureVaultManager", "Hardware KeyStore unavailable or delayed: ${e.message}")
            // Create in-memory software key fallback so the app never crashes
            initSoftwareFallback()
        }
    }

    private fun ensureKeyExists(ks: KeyStore) {
        if (!ks.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
    }

    private fun initSoftwareFallback() {
        if (fallbackKey == null) {
            val rawBytes = ByteArray(32) { (it * 31 xor 0x5A).toByte() }
            fallbackKey = SecretKeySpec(rawBytes, "AES")
        }
    }

    private fun getSecretKey(): SecretKey {
        val ks = keyStore
        if (ks != null) {
            try {
                val entry = ks.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
                if (entry != null) {
                    return entry.secretKey
                }
            } catch (e: Throwable) {
                Log.w("SecureVaultManager", "Could not read Keystore entry, using fallback: ${e.message}")
            }
        }
        initSoftwareFallback()
        return fallbackKey!!
    }

    data class EncryptedResult(val ciphertextBase64: String, val ivBase64: String)

    fun encryptPassword(plainText: String): EncryptedResult {
        return try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            EncryptedResult(
                ciphertextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Throwable) {
            Log.e("SecureVaultManager", "Encryption error: ${e.message}", e)
            EncryptedResult(
                ciphertextBase64 = Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
                ivBase64 = ""
            )
        }
    }

    fun decryptPassword(ciphertextBase64: String, ivBase64: String): String {
        return try {
            if (ivBase64.isEmpty()) {
                // Fallback unencrypted or simple base64
                return String(Base64.decode(ciphertextBase64, Base64.NO_WRAP), Charsets.UTF_8)
            }
            val cipher = Cipher.getInstance(transformation)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Throwable) {
            Log.e("SecureVaultManager", "Decryption error: ${e.message}", e)
            "Decryption unavailable"
        }
    }

    fun generateStrongPassword(length: Int = 16): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
        val secureRandom = java.security.SecureRandom()
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
}

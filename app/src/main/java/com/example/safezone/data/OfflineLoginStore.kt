package com.example.safezone.data

import android.content.Context
import android.util.Base64
import android.util.Log
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

class OfflineLoginStore(
    context: Context,
    private val accountType: String = "Personal"
) {

    data class CachedProfile(
        val userId: String,
        val email: String,
        val fullName: String,
        val accountType: String
    )

    private val appContext = context.applicationContext

    private val preferences = appContext.getSharedPreferences(
        when (accountType) {
            "Personal" -> "safezone_personal_offline_session"
            "Campus" -> "safezone_campus_offline_session"
            else -> throw IllegalArgumentException(
                "Unsupported account type: $accountType"
            )
        },
        Context.MODE_PRIVATE
    )

    private val keyStoreAlias = when (accountType) {
        "Personal" -> "SafeZonePersonalOfflinePasswordKey"
        "Campus" -> "SafeZoneCampusOfflinePasswordKey"
        else -> throw IllegalArgumentException(
            "Unsupported account type: $accountType"
        )
    }

    companion object {
        private const val TAG = "OfflineLoginStore"

        private const val KEY_USER_ID = "verified_user_id"
        private const val KEY_EMAIL = "verified_email"
        private const val KEY_FULL_NAME = "verified_full_name"
        private const val KEY_ACCOUNT_TYPE = "verified_account_type"
        private const val KEY_PASSWORD_VERIFIER =
            "encrypted_password_verifier"

        private const val SALT_LENGTH = 16
        private const val HASH_LENGTH = 32
        private const val ITERATIONS = 210_000
        private const val IV_LENGTH = 12
    }

    /**
     * Call only after Firebase authentication and Firestore
     * have confirmed the correct account type.
     *
     * A null password saves the verified profile without
     * creating a password verifier, as with Google sign-in.
     */
    fun saveVerifiedLogin(
        userId: String,
        email: String,
        fullName: String,
        password: String?
    ): Boolean {
        if (userId.isBlank() || email.isBlank()) return false

        return try {
            val editor = preferences.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email.trim())
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_ACCOUNT_TYPE, accountType)

            if (password != null) {
                if (password.isEmpty()) return false

                val salt = ByteArray(SALT_LENGTH)
                SecureRandom().nextBytes(salt)

                val passwordChars = password.toCharArray()

                val hash = try {
                    derivePasswordHash(passwordChars, salt)
                } finally {
                    passwordChars.fill('\u0000')
                }

                // Encrypt the salt and password hash together.
                val payload = salt + hash

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getOrCreateKeystoreKey()
                )

                val encryptedPayload = cipher.doFinal(payload)
                val combined = cipher.iv + encryptedPayload

                editor.putString(
                    KEY_PASSWORD_VERIFIER,
                    Base64.encodeToString(combined, Base64.NO_WRAP)
                )

                salt.fill(0)
                hash.fill(0)
                payload.fill(0)
            }

            val saved = editor.commit()

            if (!saved) {
                Log.e(TAG, "Could not save the offline login")
            }

            saved
        } catch (exception: Exception) {
            Log.e(TAG, "Could not prepare offline login", exception)
            false
        }
    }

    /**
     * Verifies the entered email and password locally.
     * Does not require Firebase or an internet connection.
     */
    fun verifyOfflineLogin(
        email: String,
        password: String
    ): CachedProfile? {
        if (email.isBlank() || password.isEmpty()) return null

        val savedEmail = preferences.getString(KEY_EMAIL, null)
            ?: return null

        val savedUserId = preferences.getString(KEY_USER_ID, null)
            ?: return null

        val savedFullName =
            preferences.getString(KEY_FULL_NAME, null) ?: "SafeZone User"

        val savedAccountType =
            preferences.getString(KEY_ACCOUNT_TYPE, null) ?: return null

        if (!savedEmail.equals(email.trim(), ignoreCase = true)) {
            return null
        }

        if (savedAccountType != accountType) return null

        val encodedVerifier =
            preferences.getString(KEY_PASSWORD_VERIFIER, null)
                ?: return null

        return try {
            val combined = Base64.decode(
                encodedVerifier,
                Base64.NO_WRAP
            )

            if (combined.size <= IV_LENGTH) return null

            val iv = combined.copyOfRange(0, IV_LENGTH)
            val ciphertext =
                combined.copyOfRange(IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKeystoreKey(),
                GCMParameterSpec(128, iv)
            )

            val payload = cipher.doFinal(ciphertext)

            if (payload.size != SALT_LENGTH + HASH_LENGTH) {
                return null
            }

            val salt = payload.copyOfRange(0, SALT_LENGTH)
            val expectedHash =
                payload.copyOfRange(SALT_LENGTH, payload.size)

            val passwordChars = password.toCharArray()

            val enteredHash = try {
                derivePasswordHash(passwordChars, salt)
            } finally {
                passwordChars.fill('\u0000')
            }

            val matches = MessageDigest.isEqual(
                expectedHash,
                enteredHash
            )

            salt.fill(0)
            expectedHash.fill(0)
            enteredHash.fill(0)
            payload.fill(0)

            if (!matches) return null

            CachedProfile(
                userId = savedUserId,
                email = savedEmail,
                fullName = savedFullName,
                accountType = savedAccountType
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Offline password verification failed", exception)
            null
        }
    }

    fun hasSavedLogin(): Boolean {
        return preferences.contains(KEY_USER_ID) &&
                preferences.contains(KEY_EMAIL) &&
                preferences.contains(KEY_PASSWORD_VERIFIER) &&
                preferences.getString(KEY_ACCOUNT_TYPE, null) == accountType
    }

    fun clearSavedLogin() {
        preferences.edit().clear().apply()

        // Deliberately leave the Keystore key in place.
        // The key alone cannot restore the cleared credentials.
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val existingKey =
            keyStore.getKey(keyStoreAlias, null) as? SecretKey

        if (existingKey != null) return existingKey

        val generator = javax.crypto.KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val specification = KeyGenParameterSpec.Builder(
            keyStoreAlias,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setKeySize(256)
            .build()

        generator.init(specification)

        return generator.generateKey()
    }

    private fun derivePasswordHash(
        password: CharArray,
        salt: ByteArray
    ): ByteArray {
        val specification = PBEKeySpec(
            password,
            salt,
            ITERATIONS,
            HASH_LENGTH * 8
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(specification)
                .encoded
        } finally {
            specification.clearPassword()
        }
    }
}
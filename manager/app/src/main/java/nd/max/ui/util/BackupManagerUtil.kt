/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.util
import nd.max.core.platform.EventLog


import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject


object BackupManager {

    private const val SECRET_KEY = "ZexshiaMaxManagerSecuredKey" 
    private const val ALGORITHM = "AES"


    fun getSocName(type: String?): String {
        return when (type) {
            "1" -> "MediaTek"
            "2" -> "Snapdragon"
            "3" -> "Exynos"
            "4" -> "Unisoc"
            "5" -> "Tensor"
            else -> "Unknown ($type)"
        }
    }


    /**
     * AES accepts only 16, 24 or 32-byte keys, but [SECRET_KEY] is 27 bytes. Passing those bytes
     * straight to `Cipher.init` throws InvalidKeyException on every call, so backup creation always
     * failed and no backup file could be read back. Hashing the same passphrase yields a valid
     * 256-bit key.
     */
    private fun aesKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(SECRET_KEY.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, ALGORITHM)
    }


    private fun encryptData(data: String): ByteArray {
        val keySpec = aesKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(data.toByteArray())
        return Base64.encode(encrypted, Base64.NO_WRAP)
    }


    private fun decryptData(data: ByteArray): String {
        val decoded = Base64.decode(data, Base64.NO_WRAP)
        val keySpec = aesKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return String(cipher.doFinal(decoded))
    }


    fun createBackup(context: Context, uri: Uri, properties: Map<String, String>): Boolean {
        return try {
            val jsonObject = JSONObject()
            properties.forEach { (key, value) -> jsonObject.put(key, value) }
            
            val encryptedData = encryptData(jsonObject.toString())
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(encryptedData)
            }
            true
        } catch (e: Exception) {
            EventLog.error("Backup", "create_backup", e)
            false
        }
    }


    fun readBackup(context: Context, uri: Uri): Map<String, String>? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArrayOutputStream()
                inputStream.copyTo(buffer)
                buffer.toByteArray()
            } ?: return null

            val decryptedJson = decryptData(bytes)
            val jsonObject = JSONObject(decryptedJson)
            
            val resultMap = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                resultMap[key] = jsonObject.getString(key)
            }
            resultMap
        } catch (e: Exception) {
            EventLog.error("Backup", "read_backup", e)
            null
        }
    }
}

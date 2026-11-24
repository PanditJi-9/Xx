package com.maxmanager.control.data

import java.security.MessageDigest

data class SignedCloudProfile(
    val id: String,
    val socPlatform: String,
    val packageName: String,
    val author: String,
    val payloadJson: String,
    val sha256Hex: String
)

object CloudProfileVerifier {
    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verify(profile: SignedCloudProfile): Boolean {
        if (profile.sha256Hex.length != 64) return false
        return sha256Hex(profile.payloadJson).equals(profile.sha256Hex, ignoreCase = true)
    }
}

class CloudProfileRepository(
    private val profiles: MutableList<SignedCloudProfile> = mutableListOf()
) {
    fun addVerified(profile: SignedCloudProfile): Boolean {
        if (!CloudProfileVerifier.verify(profile)) return false
        profiles.removeAll { it.id == profile.id }
        profiles += profile
        return true
    }

    fun findBestForDevice(socPlatform: String, packageName: String): List<SignedCloudProfile> {
        val normalizedSoc = socPlatform.trim().lowercase()
        val exact = profiles.filter {
            it.socPlatform.lowercase() == normalizedSoc && it.packageName == packageName
        }
        if (exact.isNotEmpty()) return exact
        return profiles.filter {
            it.socPlatform.equals("universal", ignoreCase = true) && it.packageName == packageName
        }
    }
}

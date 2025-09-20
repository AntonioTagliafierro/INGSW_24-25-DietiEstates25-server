package com.security.hashing
import org.apache.commons.codec.digest.DigestUtils
import java.security.SecureRandom
import org.apache.commons.codec.binary.Hex

class SHA256HashingService: HashingService {

    override fun generateSaltedHash(value: String, saltLength: Int): SaltedHash {

        val salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLength)
        val saltAsHex = Hex.encodeHexString(salt)
        val combined = saltAsHex.trim()+value.trim()
        val hash = DigestUtils.sha256Hex(combined) //password piu sicura essendo crittografata con salt e value

        val saltedHash = SaltedHash(
            hash = hash,
            salt = saltAsHex
        )

        return saltedHash
    }

    override fun verify(value: String, saltedHash: SaltedHash): Boolean {

        val combined = saltedHash.salt!!.trim()+value.trim()

        val hashRequest = DigestUtils.sha256Hex(combined)

        return hashRequest == saltedHash.hash
    }
}
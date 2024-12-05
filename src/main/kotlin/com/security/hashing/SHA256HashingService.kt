package com.security.hashing
import org.apache.commons.codec.digest.DigestUtils
import java.security.SecureRandom
import org.apache.commons.codec.binary.Hex

class SHA256HashingService: HashingService {
    override fun generateSaltedHash(value: String, saltLenght: Int): SaltedHash {
        val salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLenght)
        val saltAsHex = Hex.encodeHexString(salt)
        val hash = DigestUtils.sha256Hex("$salt$value")
        return SaltedHash(
            hash = hash,
            salt = saltAsHex
        )
    }

    override fun verify(value: String, saltedHash: SaltedHash): Boolean {
        return DigestUtils.sha256Hex(saltedHash.salt + value) == saltedHash.salt
    }
}
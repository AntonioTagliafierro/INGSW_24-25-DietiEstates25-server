package com.security.hashing

interface HashingService {
    fun generateSaltedHash(value: String, saltLenght: Int = 32): SaltedHash
    fun verify(value: String, saltedHash: SaltedHash): Boolean
}
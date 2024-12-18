package com.utility

import java.security.SecureRandom

class GeneratePassword {

    fun generateRandomPassword(length: Int = 12): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
        val random = SecureRandom()
        return (1..length)
            .map { characters[random.nextInt(characters.length)] }
            .joinToString("")
    }
}
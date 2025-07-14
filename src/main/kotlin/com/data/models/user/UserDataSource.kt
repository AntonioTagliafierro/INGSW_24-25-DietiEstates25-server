package com.data.models.user

interface UserDataSource {

    suspend fun getUserByEmail(email: String): User?
    suspend fun insertUser(user: User): Boolean
    suspend fun checkUserByEmail(user: User): Boolean
    suspend fun updateUserPassword(email: String, newHash: String?, newSalt: String?): Boolean

}
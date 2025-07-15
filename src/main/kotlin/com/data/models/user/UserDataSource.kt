package com.data.models.user

import com.data.requests.AuthRequest

interface UserDataSource {

    suspend fun getUserByEmail(email: String): User?
    suspend fun insertUser(user: User): Boolean
    suspend fun checkUserByEmail(user: User): Boolean
    suspend fun updateUserPassword(email: String, newHash: String?, newSalt: String?): Boolean
    suspend fun verifyThirdPartyUser(request: AuthRequest): Result<User>
}
package com.data.models.user

interface UserDataSource {

    suspend fun getUserByEmail(email: String): User?
    suspend fun insertUser(user: User): Boolean
    suspend fun checkUserByEmail(user: User): Boolean
}
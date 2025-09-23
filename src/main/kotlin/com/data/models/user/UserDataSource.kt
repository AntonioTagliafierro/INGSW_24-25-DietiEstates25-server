package com.data.models.user

import com.data.models.agency.Agency
import com.data.models.image.ImageDataSource
import com.data.requests.AuthRequest
import com.security.hashing.HashingService

interface UserDataSource {

    suspend fun getUserByEmail(email: String): User?
    suspend fun insertUser(user: User): Boolean
    suspend fun checkUserByEmail(user: User): Boolean
    suspend fun updateUserPassword(email: String, newHash: String?, newSalt: String?): Boolean
    suspend fun getUserById(userId: String): User?
    suspend fun getAllUsers(): List<User>
    suspend fun getUsersByRole(role : String): List<User>
    suspend fun getAgencyUsers(userIds: List<String>): List<User>
    suspend fun updateUsername(email: String, username: String): Boolean
    suspend fun updateFullName(email: String, value: String): Boolean
    suspend fun updateUserRole(email: String , role : Role) :Boolean

    suspend fun ensureSysAdmin(hashingService: HashingService, imageDataSource: ImageDataSource)
}
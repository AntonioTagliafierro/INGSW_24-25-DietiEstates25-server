package com.data.models.image

import java.io.InputStream

interface ImageDataSource {
     suspend fun getHouseImages(houseId: String): List<String>
     suspend fun getUserProfileImage(userId: String): String?
     suspend fun updateHouseImages(houseId: String, base64Images: List<String>): Boolean
     suspend fun updateIdProfileImage(profilePicUserId: String, base64Image: String): Boolean
}
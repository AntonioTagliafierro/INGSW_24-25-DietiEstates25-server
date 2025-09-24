package com.data.models.image

import org.intellij.lang.annotations.Identifier
import java.io.InputStream

interface ImageDataSource {
     suspend fun getHouseImages(houseId: String): List<String>
     suspend fun getUserProfileImage(userId: String): String?
     suspend fun updateHouseImages(houseId: String, base64Images: List<String>): Boolean
     suspend fun updatePpById(ownerIdentifier: String, base64Image: String): Boolean

     suspend fun deleteImages(userId: String): Boolean
}
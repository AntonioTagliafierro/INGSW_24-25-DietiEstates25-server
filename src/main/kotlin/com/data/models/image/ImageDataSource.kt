package com.data.models.image



interface ImageDataSource {
     suspend fun getHouseImages(houseId: String): List<String>
     suspend fun getUserProfileImage(userId: String): String?
     suspend fun updateHouseImages(houseId: String, base64Images: List<String>): Boolean
     suspend fun updatePpById(ownerIdentifier: String, base64Image: String): Boolean

     suspend fun deleteImages(userId: String): Boolean

     suspend fun getHouseImagesByIds(houseIds: List<String>): Map<String, List<String>>
}
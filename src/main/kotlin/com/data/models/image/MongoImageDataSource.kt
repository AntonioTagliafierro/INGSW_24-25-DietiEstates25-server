package com.data.models.image



import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList


class MongoImageDataSource(
    db: MongoDatabase
) : ImageDataSource {

    private val images = db.getCollection<StoredImage>("images")

    override suspend fun updatePpById(ownerIdentifier: String, base64Image: String): Boolean {
        images.deleteMany(Filters.eq("ownerId", ownerIdentifier)) // Rimuove l'immagine esistente
        val result = images.insertOne(StoredImage(ownerId = ownerIdentifier, base64 = base64Image))
        return result.wasAcknowledged()
    }

    override suspend fun deleteImages(userId: String): Boolean {
        val result = images.deleteMany(Filters.eq("ownerId", userId))

        return result.wasAcknowledged()
    }

    override suspend fun updateHouseImages(houseId: String, base64Images: List<String>): Boolean {
        if (base64Images.size > 2) throw IllegalArgumentException("Massimo 2 immagini consentite")
        images.deleteMany(Filters.eq("ownerId", houseId)) // Rimuove immagini vecchie
        val newImages = base64Images.map { StoredImage(ownerId = houseId, base64 = it) }
        val result = images.insertMany(newImages)
        return result.wasAcknowledged()
    }

    override suspend fun getUserProfileImage(userId: String): String? {
        return images.find(Filters.eq("ownerId", userId))
            .firstOrNull()
            ?.base64
    }

    override suspend fun getHouseImages(houseId: String): List<String> {
        return images.find(Filters.eq("ownerId", houseId))
            .toList()
            .map { it.base64 }
    }

    override suspend fun getHouseImagesByIds(houseIds: List<String>): Map<String, List<String>> {
        if (houseIds.isEmpty()) return emptyMap()

        val results = images.find(Filters.`in`("ownerId", houseIds)).toList()

        return results.groupBy { it.ownerId }
            .mapValues { entry -> entry.value.map { it.base64 } }
    }


}


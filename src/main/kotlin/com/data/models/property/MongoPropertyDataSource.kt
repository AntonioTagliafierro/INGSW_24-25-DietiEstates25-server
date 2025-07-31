package com.data.models.property

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class MongoPropertyDataSource(
    db: MongoDatabase
) : PropertyDataSource {

    private val properties = db.getCollection<Property>("properties")

    override suspend fun insertProperty(property: Property): Boolean {
        return properties.insertOne(property).wasAcknowledged()
    }

    override suspend fun getProperties(): List<Property> {
        return properties.find().toList()
    }




}

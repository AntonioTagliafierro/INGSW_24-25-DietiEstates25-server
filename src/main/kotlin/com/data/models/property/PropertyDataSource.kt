package com.data.models.property

interface PropertyDataSource {
    suspend fun insertProperty(property: Property): Boolean
    suspend fun getProperties(): List<Property>

}
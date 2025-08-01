package com.data.models.agency


interface AgencyDataSource {
    suspend fun insertAgency(agency: Agency): Boolean
    suspend fun insertAgencyUser(agencyUser: AgencyUser): Boolean
    suspend fun getAgency(nameAgency: String): Agency?
}
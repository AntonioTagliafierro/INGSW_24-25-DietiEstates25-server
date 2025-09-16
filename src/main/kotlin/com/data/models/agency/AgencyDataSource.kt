package com.data.models.agency


interface AgencyDataSource {
    suspend fun insertAgency(agency: Agency): Boolean
    suspend fun insertAgencyUser(agencyUser: AgencyUser): Boolean
    suspend fun getAgency(nameAgency: String): Agency?

    suspend fun updateAgencyState(userId: String): Boolean
    suspend fun deleteAgency(userId: String): Boolean
    suspend fun getAllAgencies(): List<Agency>

}
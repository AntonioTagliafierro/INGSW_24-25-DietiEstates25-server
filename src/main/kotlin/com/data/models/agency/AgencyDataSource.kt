package com.data.models.agency

import com.data.models.user.User


interface AgencyDataSource {
    suspend fun insertAgency(agency: Agency): Boolean
    suspend fun insertAgencyUser(agencyUser: AgencyUser): Boolean
    suspend fun getAgency(nameAgency: String): Agency?
    suspend fun getAgencyByEmail(email: String): Agency?
    suspend fun getAgencyUserIds(agencyId : String): List<String>
    suspend fun updateAgencyState(userId: String): Boolean
    suspend fun deleteAgency(userId: String): Boolean
    suspend fun getAllAgencies(): List<Agency>
    suspend fun getAgencyByAgentId(agentId: String) : Agency?

}
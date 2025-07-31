package com.data.models.agency

import com.data.models.admin.Admin
import com.data.models.user.User

interface AgencyDataSource {
    suspend fun insertAgency(agency: Agency): Boolean
    suspend fun insertAgencyUser(agencyUser: AgencyUser): Boolean
    suspend fun getAgency(nameAgency: String): Agency?
}
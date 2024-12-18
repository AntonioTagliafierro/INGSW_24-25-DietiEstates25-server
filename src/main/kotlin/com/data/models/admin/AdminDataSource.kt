package com.data.models.admin

interface AdminDataSource {

    suspend fun getAdminByEmail(email: String): Admin?
    suspend fun insertAdmin(user: Admin): Boolean
}
package com.data.models.activity


interface ActivityDataSource {

    suspend fun insertActivity(activity: Activity) : Boolean
    suspend fun getAllActivityByUser(userId: String) : List<Activity>
    suspend fun deleteActivities(userId: String): Boolean


    fun textDECLINED( userEmail : String ) : String
    fun textACCEPTED( userEmail : String ) : String
    fun textINSERT( userEmail : String ) : String
}
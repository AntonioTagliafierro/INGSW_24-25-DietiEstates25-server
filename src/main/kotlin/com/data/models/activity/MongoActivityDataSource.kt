package com.data.models.activity

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList

class MongoActivityDataSource(
    db: MongoDatabase,
) : ActivityDataSource {

    private val activities = db.getCollection<Activity>("activities")

    override suspend fun insertActivity(activity: Activity): Boolean {
        return try {
            val result = activities.insertOne(activity)
            result.wasAcknowledged()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getAllActivityByUser(userId: String): List<Activity> {
        return try {
            activities.find(Filters.eq("userId", userId)).toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    override suspend fun deleteActivities(userId: String): Boolean {

        val deleteUser = activities.deleteMany(Filters.eq("userId", userId))

        return deleteUser.deletedCount > 0
    }


    override fun textACCEPTED( userEmail : String ) : String{
        return "You Accepted a request by: $userEmail"
    }

    override fun textINSERT( userEmail : String ) : String{
        return "You Added an account : $userEmail"
    }

    override fun textDECLINED( userEmail : String ) : String{
        return "You Declined a request of an agenct by: $userEmail"
    }

}
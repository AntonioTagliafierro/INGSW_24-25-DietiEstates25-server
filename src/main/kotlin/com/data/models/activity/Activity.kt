package com.data.models.activity

import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Serializable
enum class ActivityType(val label: String) {

    @SerialName("INSERT")
    INSERT("INSERT"),

    @SerialName("ACCEPTED")
    ACCEPTED("ACCEPTED"),

    @SerialName("DECLINED")
    DECLINED("DECLINED"),

    @SerialName("VIEWED")
    VIEWED("VIEWED"),

    @SerialName("OFFERED")
    OFFERED("OFFERED"),

    @SerialName("BOOKED")
    BOOKED("BOOKED")
}

@Serializable
data class Activity(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    private val userId: String,
    private val type: ActivityType,
    private val text: String,
    private val date: String,
){

    constructor(userId: String, type: ActivityType, text: String) : this(
        id = ObjectId.get(),
        userId = userId,
        type = type,
        text = text,
        date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yy"))
    )

}


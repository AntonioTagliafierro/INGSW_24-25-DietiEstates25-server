package com

import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.pojo.PojoCodecProvider
/*
fun createDatabase(databaseName: String = "INGSW"): MongoDatabase {
    // Configura il codec registry manualmente
    val pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build()
    val pojoCodecRegistry: CodecRegistry = CodecRegistries.fromProviders(pojoCodecProvider)

    // Configura le impostazioni del client MongoDB, incluso l'host e la porta personalizzati
    val clientSettings = MongoClientSettings.builder()
        .applyToClusterSettings { builder ->
            builder.hosts(listOf(ServerAddress("ingsw.lehlq.mongodb.net", 27017))) // Cambia la porta qui
        }
        .codecRegistry(pojoCodecRegistry) // Usa il codec registry appena creato
        .build()

    // Crea il client MongoDB con le impostazioni specificate
    val mongoClient = MongoClients.create(clientSettings)

    // Restituisci il database specificato
    return mongoClient.getDatabase(databaseName)
}

fun ConnectToMongoDB(): MongoDatabase {

    /*val user = environment.config.tryGetString("dietiestates25")
    val password = environment.config.tryGetString("uIYxoeVZmUJMVPTv")
    val host = environment.config.tryGetString("db.mongo.host") ?: "ingsw"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27017"
    val maxPoolSize = environment.config.tryGetString("db.mongo.maxPoolSize")?.toInt() ?: 20
    val databaseName = environment.config.tryGetString("db.mongo.database.name") ?: "myDatabase"

    val credentials = user?.let { userVal -> password?.let { passwordVal -> "$userVal:$passwordVal@" } }.orEmpty()
    */

    val uri = "mongodb+srv://dietiestates25:<uIYxoeVZmUJMVPTv>@ingsw.lehlq.mongodb.net/?retryWrites=true&w=majority&appName=INGSW"

    val mongoClient = MongoClients.create(uri)
    val database = mongoClient.getDatabase("myDatabase")


    return database
}*/


fun createDatabase(databaseName: String = "myDatabase"): MongoDatabase {
    // Configura manualmente il CodecRegistry
    val pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build()
    val pojoCodecRegistry: CodecRegistry = CodecRegistries.fromProviders(pojoCodecProvider)

    // Configura il client MongoDB con il CodecRegistry personalizzato
    val clientSettings = MongoClientSettings.builder()
        .applyToClusterSettings { builder ->
            builder.hosts(listOf(com.mongodb.ServerAddress("127.0.0.1", 27017)))
        }
        .codecRegistry(pojoCodecRegistry) // Usa solo il CodecRegistry personalizzato
        .build()

    // Crea il client e il database
    val mongoClient = MongoClients.create(clientSettings)
    return mongoClient.getDatabase(databaseName)
}


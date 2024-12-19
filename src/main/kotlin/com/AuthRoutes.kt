package com


import com.data.models.user.User
import com.data.models.user.UserDataSource
import com.data.requests.AuthRequest
import com.data.responses.AuthResponse
import com.security.hashing.HashingService
import com.security.hashing.SaltedHash
import com.security.token.TokenClaim
import com.security.token.TokenConfig
import com.security.token.TokenService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.signUp(
    hashingService: HashingService,
    userDataSource: UserDataSource
){
    // Endpoint POST per la registrazione degli utenti
    post("signup"){
        // Recupera e valida la richiesta (AuthRequest) ricevuta dal client
        val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: kotlin.run {
            // Risponde con HTTP 400 (Bad Request) se il payload non è valido o assente
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        // Verifica che i campi non siano vuoti e che la password abbia almeno 8 caratteri
        val areFieldsBlank = request.email.isBlank() || request.password.isBlank()
        val isPwTooShort = request.password.length < 8
        if(areFieldsBlank || isPwTooShort){
            // Risponde con HTTP 409 (Conflict) se i controlli falliscono
            call.respond(HttpStatusCode.Conflict)
            return@post
        }

        // Genera un hash della password utilizzando il servizio di hashing
        val saltedHash = hashingService.generateSaltedHash(request.password)
        val user = User(
            email = request.email,
            password = saltedHash.hash,
            salt = saltedHash.salt
        )

        // Tenta di inserire il nuovo utente nel database
        val wasAcknowledged = userDataSource.insertUser(user)
        if(!wasAcknowledged){
            // Risponde con HTTP 409 (Conflict) in caso di errore
            call.respond(HttpStatusCode.Conflict)
        }

        // Risponde con HTTP 200 (OK) se tutto va a buon fine
        call.respond(HttpStatusCode.OK)
    }
}


fun Route.signIn(
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig
){
    // Endpoint POST per l'accesso degli utenti
    post("signin"){
        // Recupera e valida la richiesta (AuthRequest) ricevuta dal client
        val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: kotlin.run {
            // Risponde con HTTP 400 (Bad Request) se il payload non è valido o assente
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        // Recupera l'utente dal database utilizzando l'email fornita
        val user = userDataSource.getUserByEmail(request.email)

        // Verifica se l'utente esiste
        if(user == null){
            // Risponde con HTTP 409 (Conflict) se l'utente non esiste
            call.respond(HttpStatusCode.Conflict, "Incorrect email")
            return@post
        }else{
            println("Nome utente verificato con successo")
        }


        // Verifica la validità della password fornita
        val isValidPassword = hashingService.verify(
            value = request.password,
            saltedHash = SaltedHash(
                hash = user.password,
                salt = user.salt
            )
        )

        if(!isValidPassword){
            // Risponde con HTTP 409 (Conflict) se la password non è corretta
            call.respond(HttpStatusCode.Conflict, "Incorrect password")
            return@post
        }else{
            println("Password verificata con successo")
        }

        // Genera un token JWT per l'utente autenticato
        val token = tokenService.generate(
            config = tokenConfig,
            TokenClaim(
                name = "userId",
                value = user.id.toString()
            )
        )

        // Risponde con HTTP 200 (OK) e restituisce il token al client
        call.respond(
            status = HttpStatusCode.OK,
            message = AuthResponse(
                token = token,
            )
        )
    }
}

fun Route.authenticate(){
    // Autentica una richiesta generica
    authenticate {
        get("authenticate") {
            // Risponde con HTTP 200 (OK) se l'utente è autenticato
            call.respond(HttpStatusCode.OK)
        }
    }
}


fun Route.getSecretInfo() {
    // Endpoint protetto per ottenere informazioni riservate

    println("ciao")
    authenticate {
        get("secret") {
            // Recupera il JWTPrincipal dalla richiesta autenticata
            val principal = call.principal<JWTPrincipal>()

            // Recupera un claim specifico (userId) dal token JWT
            val userId = principal?.getClaim("userId", String::class)

            // Risponde con HTTP 200 (OK) e restituisce l'informazione riservata
            call.respond(HttpStatusCode.OK, "Your userId is $userId")
        }
    }


}




package com

import com.security.state.*
import com.data.models.admin.Admin
import com.data.models.admin.AdminDataSource
import com.data.models.user.User
import com.data.models.user.UserDataSource
import com.data.requests.AgencyRegistrationRequest
import com.data.requests.AuthRequest
import com.data.responses.AuthResponse
import com.data.models.user.*
import com.data.requests.GitHubAuthRequest
import com.security.hashing.HashingService
import com.security.hashing.SaltedHash
import com.security.token.*
import com.utility.EmailService
import com.utility.GeneratePassword
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.util.*

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

        val user = LocalUser(
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

fun Route.signUpAdmin(
    hashingService: HashingService,
    adminDataSource: AdminDataSource
) {
    post("addadminagency"){

        // Recupera e valida la richiesta (AuthRequest) ricevuta dal client
        val request = kotlin.runCatching { call.receiveNullable<AgencyRegistrationRequest>() }.getOrNull() ?: kotlin.run {
            // Risponde con HTTP 400 (Bad Request) se il payload non è valido o assente
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        // Verifica che i campi non siano vuoti
        val areFieldsBlank = request.email.isBlank() || request.agencyName.isBlank()

        if(areFieldsBlank){
            // Risponde con HTTP 409 (Conflict) se i controlli falliscono
            call.respond(HttpStatusCode.Conflict)
            return@post
        }

        val generatedPassword = GeneratePassword().generateRandomPassword()

        val saltedHash = hashingService.generateSaltedHash(generatedPassword)

        val admin = Admin(
            email = request.email,
            password = saltedHash.hash ?: "",
            salt = saltedHash.salt ?: "",
            agencyName = request.agencyName,
            type = "Admin"
        )

        val wasAcknowledged = adminDataSource.insertAdmin(admin)

        if(!wasAcknowledged){
            // Risponde con HTTP 409 (Conflict) in caso di errore
            call.respond(HttpStatusCode.Conflict)
        }

        // Risponde con HTTP 200 (OK) se tutto va a buon fine e manda la password via email
        EmailService().sendPasswordEmail(request.email, generatedPassword)
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

        // Verifica se l'utente esiste e se è un LocalUser
        if (user !is LocalUser) {
            call.respond(HttpStatusCode.Conflict, "Utente loggato con terzeparti")
            return@post
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

fun Route.state(){
    get("/generate-state") {
        val state = UUID.randomUUID().toString() // Genera uno stato unico
        // Salva lo stato generato (ad esempio in memoria o su database) per la successiva verifica
        StateStore.save(state) // Pseudo funzione per salvare
        call.respondText(state, ContentType.Text.Plain)
    }

}fun Route.githubAuthVerification(
    gitHubOAuthService: GitHubOAuthService,
    userDataSource: UserDataSource,
) {

    post("/auth/github") {
        println("[GitHubAuth] Received POST request at /auth/github")

        // Ricevi il corpo della richiesta
        val request = call.receive<GitHubAuthRequest>()
        val code = request.code
        val state = request.state

        // Verifica che il codice e lo stato siano presenti
        if (code.isBlank() || state.isBlank()) {
            println("[GitHubAuth] Missing code or state: code=$code, state=$state")
            call.respond(HttpStatusCode.BadRequest, "Code or state missing")
            return@post
        }

        // Verifica lo stato con lo StateStore (previene attacchi CSRF)
        println("[GitHubAuth] Verifying state: $state")
        val isValidState = StateStore.get(state)
        if (!isValidState) {
            println("[GitHubAuth] Invalid or expired state: $state")
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired state")
            return@post
        }

        try {
            // Scambia il codice per ottenere un access token
            println("[GitHubAuth] Exchanging code for access token: $code")
            val accessToken = gitHubOAuthService.getAccessToken(code)
                ?: throw IllegalStateException("Failed to retrieve access token")

            // Usa l'access token per ottenere i dati dell'utente
            println("[GitHubAuth] Fetching user info with access token: $accessToken")
            val userInfo = gitHubOAuthService.getUserInfo(accessToken)
                ?: throw IllegalStateException("Failed to retrieve user info")
            println("[GitHubAuth] User info retrieved successfully: $userInfo")

            println("[GitHubAuth] Fetching user email with access token: $accessToken")
            val email = gitHubOAuthService.getPrimaryEmail(accessToken)
                ?: throw IllegalStateException("Failed to retrieve user email")
            println("[GitHubAuth] User email retrieved successfully: $email")

            var user : User? = userDataSource.getUserByEmail(email)

            if( user == null ){
                // Crea un'istanza di ThirdPartyUser

                val thirdPartyUser : User = ThirdPartyUser(
                    username = userInfo.login,
                    email = email,
                    provider = "github"
                )

                println("[GitHubAuth] ThirdPartyUser instance created: $thirdPartyUser")

                userDataSource.insertUser(thirdPartyUser)
                println("[GitHubAuth] User saved to database: ${thirdPartyUser.getUsername()}")
                user = thirdPartyUser

            }else if (user.type == "localUser"){

                call.respond(HttpStatusCode.Conflict, "Non puoi loggare con le terze parti perchè gia iscritto")
                return@post

            }

            // Rispondi con il payload dell'utente
            call.respond(HttpStatusCode.OK, user)

        } catch (e: Exception) {
            println("[GitHubAuth] Error during GitHub authentication: ${e.localizedMessage}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, "Authentication failed")
        }
    }

    get("/callback/github") {
        println("[GitHubCallback] Received GET request at /callback/github")

        // Retrieve "code" and "state" from the query string
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]

        if (code != null && state != null) {
            val redirectUri = "dietiestates25://callback/github?code=$code&state=$state"
            println("[GitHubCallback] Redirecting to client with URI: $redirectUri")
            call.respondRedirect(redirectUri)
        } else {
            val queryParams = call.request.queryParameters.entries().joinToString(", ") { "${it.key}=${it.value}" }
            println("[GitHubCallback] Missing parameters: code=$code, state=$state. QueryParams: $queryParams")
            call.respondText(
                "Authorization code or state not found. QueryParams: $queryParams",
                status = HttpStatusCode.BadRequest
            )
        }
    }
}



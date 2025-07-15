package com

import com.security.state.*
import com.data.models.admin.Admin
import com.data.models.admin.AdminDataSource
import com.data.models.user.UserDataSource
import com.data.requests.AgencyRegistrationRequest
import com.data.requests.AuthRequest
import com.data.responses.AuthResponse
import com.data.models.user.*
import com.data.requests.GitHubAuthRequest
import com.security.hashing.HashingService
import com.security.hashing.SaltedHash
import com.security.token.*
import com.service.EmailService
import com.service.GeneratePassword
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.userAuth(
    hashingService: HashingService,
    userDataSource: UserDataSource,
    tokenService: TokenService,
    tokenConfig: TokenConfig
){

    post("/auth/thirdPartyUser") {
        val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Dati mancanti o malformati.")
            return@post
        }

        val result = userDataSource.verifyThirdPartyUser(request)

        result.fold(
            onSuccess = { user ->
                // genera token come in /signin
                val token = tokenService.generate(
                    config = tokenConfig,
                    TokenClaim("userId",   user.id.toString()),
                    TokenClaim("username", user.getUsername()),
                    TokenClaim("type",     user.type)
                )

                call.respond(HttpStatusCode.OK, AuthResponse(token = token))
            },
            onFailure = { error ->
                call.respond(HttpStatusCode.Conflict, error.message ?: "Errore non puoi loggare")
            }
        )
    }

    post("/auth/signup") {
        // 1. Ricezione sicura del body JSON
        val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Dati mancanti o malformati.")
            return@post
        }

        // 2. Validazione base dei campi DA RIVALUTARE TODO
        val areFieldsBlank = request.email.isBlank()

        if (areFieldsBlank) {
            call.respond(HttpStatusCode.Conflict, "Email o password vuoti.")
            return@post
        }

        if ( userDataSource.getUserByEmail(request.email) != null ){
            call.respond(HttpStatusCode.Conflict, "Utente gia registrato.")
            return@post
        }

        // 3. Hash della password
        val saltedHash = hashingService.generateSaltedHash(request.password!!)

        val user = User(
            email = request.email,
            password = saltedHash.hash,
            salt = saltedHash.salt
        )


        // 4. Inserimento nel DB
        val wasAcknowledged = userDataSource.insertUser(user)
        if (!wasAcknowledged) {
            call.respond(HttpStatusCode.Conflict, "Email già registrata.")
            return@post
        }

        // 5. Tutto ok
        call.respond(HttpStatusCode.OK, "Registrazione completata con successo.")
    }

    post("/auth/signin") {
        val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
            return@post
        }

        val user = userDataSource.getUserByEmail(request.email)

        if (user == null) {
            call.respond(HttpStatusCode.Conflict, "Email non registrata.")
            return@post
        }

        if (user.type == "thirdPartyUser") {
            call.respond(HttpStatusCode.Conflict, "Utente registrato con provider esterno.")
            return@post
        }

        // Verifica password usando hash + salt salvati nel DB
        val isValidPassword = hashingService.verify(
            value = request.password!!,
            saltedHash = SaltedHash(
                hash = user.password,
                salt = user.salt
            )
        )

        if (!isValidPassword) {
            call.respond(HttpStatusCode.Conflict, "Password errata.")
            return@post
        }

        // Genera token JWT
        val token = tokenService.generate(
            config = tokenConfig,
            TokenClaim("userId",   user.id.toString()),
            TokenClaim("username", user.getUsername()),
            TokenClaim("type",     user.type)
        )

        call.respond(HttpStatusCode.OK, AuthResponse(token = token))
    }

    post("/auth/reset-password") {
        val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Invalid request")
            return@post
        }

        // Verifica parametri
        if (
            request.email.isBlank() ||
            request.newPassword!!.isBlank() ||
            request.newPassword.length < 8
        ) {
            call.respond(HttpStatusCode.BadRequest, "Invalid or missing fields")
            return@post
        }

        // Trova l'utente
        val user = userDataSource.getUserByEmail(request.email)

        if (user == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
            return@post
        }


        // Verifica vecchia password
        val isOldPasswordValid = hashingService.verify(
            value = request.password!!,
            saltedHash = SaltedHash(
                hash = user.password,
                salt = user.salt
            )
        )

        if (!isOldPasswordValid) {
            call.respond(HttpStatusCode.Unauthorized, "Incorrect old password")
            return@post
        }

        // Hash della nuova password
        val newHashed = hashingService.generateSaltedHash(request.newPassword)

        val updateResult = userDataSource.updateUserPassword(
            email = user.getEmail(),
            newHash = newHashed.hash,
            newSalt = newHashed.salt
        )

        if (!updateResult) {
            call.respond(HttpStatusCode.InternalServerError, "Failed to update password")
            return@post
        }

        call.respond(HttpStatusCode.OK, "Password updated successfully")
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


            val result = userDataSource.verifyThirdPartyUser(
                AuthRequest(
                    email = email,
                    provider = "github",
                    username = userInfo.login,
                    password = "github",
                )
            )

            result.fold(
                onSuccess = { user ->
                    call.respond(HttpStatusCode.OK, user)
                },
                onFailure = { error ->
                    call.respond(HttpStatusCode.Conflict, error.message ?: "Errore non puoi loggare")
                }
            )

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



package com

import com.security.state.*
import com.data.models.agency.Agency
import com.data.models.agency.AgencyDataSource
import com.data.models.agency.AgencyUser
import com.data.models.image.ImageDataSource
import com.data.models.user.UserDataSource
import com.data.requests.AuthRequest
import com.data.models.user.*
import com.data.requests.GitHubAuthRequest
import com.data.requests.ImageRequest
import com.data.responses.TokenResponse
import com.data.responses.UserResponse
import com.security.hashing.HashingService
import com.security.hashing.SaltedHash
import com.security.token.*
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

        var user = userDataSource.getUserByEmail(request.email)

        if (user == null) {
            val saltedHash = hashingService.generateSaltedHash("fldrvrqrdgroir")
            userDataSource.insertUser(
                User(
                    email = request.email,
                    username = request.username,
                    password = saltedHash.hash,
                    salt = saltedHash.salt
                ))
            user = userDataSource.getUserByEmail(request.email)
        }

        // Genera il token JWT
        val token = generateTokenForUser(user!!, tokenService, tokenConfig)

        // Rispondi con JSON { "token": "<jwt>" }
        call.respond(HttpStatusCode.OK, TokenResponse(token = token))
    }

    post("/auth/signup") {

        val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Dati mancanti o malformati.")
            return@post
        }

        val areFieldsBlank = request.email.isBlank()

        if (areFieldsBlank) {
            call.respond(HttpStatusCode.Conflict, "Email o password vuoti.")
            return@post
        }

        if ( userDataSource.getUserByEmail(request.email) != null ){
            call.respond(HttpStatusCode.Conflict, "Utente gia registrato.")
            return@post
        }

        val saltedHash = hashingService.generateSaltedHash(request.password!!)

        var user = User(
            email = request.email,
            password = saltedHash.hash,
            salt = saltedHash.salt
        )

        val wasAcknowledged = userDataSource.insertUser(user)
        if (!wasAcknowledged) {
            call.respond(HttpStatusCode.Conflict, "Errore durante l'iserimento")
            return@post
        }

        user = userDataSource.getUserByEmail(request.email)!!

        val token = generateTokenForUser(user, tokenService, tokenConfig)

        call.respond(HttpStatusCode.OK, TokenResponse(token = token))
    }

    post("/auth/signin") {
        val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
            return@post
        }

        val user = userDataSource.getUserByEmail(request.email)

        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized, "Accesso fallito: credenziali errate.")
            return@post
        }

        val isValidPassword = hashingService.verify(
            value = request.password ?: "",
            saltedHash = SaltedHash(hash = user.password, salt = user.salt)
        )

        if (!isValidPassword) {
            call.respond(HttpStatusCode.Unauthorized, "Accesso fallito: credenziali errate.")
            return@post
        }

        // Genera il token JWT
        val token = generateTokenForUser(user, tokenService, tokenConfig)

        // Rispondi con JSON { "token": "<jwt>" }
        call.respond(HttpStatusCode.OK, TokenResponse(token = token))
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

fun Route.agencyRequests(
    hashingService: HashingService,
    userDataSource: UserDataSource,
    agencyDataSource: AgencyDataSource,
){
    post("/agency-admin-request"){
        val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
            return@post
        }

        var user = userDataSource.getUserByEmail(request.email)

        if (user == null) {
            val saltedHash = hashingService.generateSaltedHash(request.password!!)

            val wasAcknowledged = userDataSource.insertUser(
                User(
                    email = request.email,
                    password = saltedHash.hash,
                    salt = saltedHash.salt
                )
            )

            if (!wasAcknowledged) {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'iserimento")
                return@post
            }

            user = userDataSource.getUserByEmail(request.email)
        }

        var wasAcknowledged = agencyDataSource.insertAgency(
            Agency(
                name = request.agencyName!!
            )
        )

        if (!wasAcknowledged) {
            call.respond(HttpStatusCode.Conflict, "Errore durante l'iserimento")
            return@post
        }
        val agency = agencyDataSource.getAgency(request.agencyName)

        wasAcknowledged = agencyDataSource.insertAgencyUser(
            AgencyUser(
                agencyId = agency!!.id.toString(),
                userId = user!!.id.toString(),
                role = "AGENCY_ADMIN"
            )
        )

        if (!wasAcknowledged) {
            call.respond(HttpStatusCode.Conflict, "Errore durante l'iserimento")
            return@post
        }

        call.respond(HttpStatusCode.OK, "Operazione completata con successo")

    }
}

fun Route.authenticate(
    userDataSource: UserDataSource
){
    // Autentica una richiesta generica
    authenticate {
        get("authenticate") {
            // Risponde con HTTP 200 (OK) se l'utente è autenticato
            call.respond(HttpStatusCode.OK)
        }

        get("/auth/me") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", String::class)

            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, "Token non valido")
                return@get
            }

            val user = userDataSource.getUserById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "Utente non trovato")
                return@get
            }

            call.respond(
                UserResponse(
                    id = user.id.toString(),
                    username = user.getUsername(),
                    name = user.name,
                    surname = user.surname,
                    email = user.getEmail(),
                    type = user.type
                )
            )
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
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig
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

            var user = userDataSource.getUserByEmail(email)

            if (user == null) {
                val saltedHash = hashingService.generateSaltedHash("fldrvrqrdgroir")
                userDataSource.insertUser(
                    User(
                    email = email,
                    username = userInfo.login,
                    password = saltedHash.hash,
                    salt = saltedHash.salt
                ))
                user = userDataSource.getUserByEmail(email)
            }

            val token = tokenService.generate(
                config = tokenConfig,
                TokenClaim("userId",   user!!.id.toString()),
                TokenClaim("username", user.getUsername()),
                TokenClaim("email", user.getEmail()),
                TokenClaim("type",     user.type)
            )

            call.respond(HttpStatusCode.OK, TokenResponse(token = token))


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



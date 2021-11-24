package no.nav.altinn.admin

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.OAuthServerSettings
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.utils.CacheControl
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.error
import io.prometheus.client.hotspot.DefaultExports
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import no.nav.altinn.admin.api.nais
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Contact
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Information
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.Swagger
import no.nav.altinn.admin.api.nielsfalk.ktor.swagger.SwaggerUi
import no.nav.altinn.admin.client.MaskinportenClient
import no.nav.altinn.admin.common.API_V1
import no.nav.altinn.admin.common.API_V2
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.common.randomUuid
import no.nav.altinn.admin.service.alerts.ExpireAlerts
import no.nav.altinn.admin.service.correspondence.AltinnCorrespondenceService
import no.nav.altinn.admin.service.correspondence.correspondenceAPI
import no.nav.altinn.admin.service.dq.AltinnDQService
import no.nav.altinn.admin.service.dq.dqAPI
import no.nav.altinn.admin.service.login.UserInfo
import no.nav.altinn.admin.service.login.UserSession
import no.nav.altinn.admin.service.login.loginAPI
import no.nav.altinn.admin.service.owner.ownerApi
import no.nav.altinn.admin.service.prefill.AltinnPrefillService
import no.nav.altinn.admin.service.prefill.prefillAPI
import no.nav.altinn.admin.service.receipt.AltinnReceiptService
import no.nav.altinn.admin.service.receipt.receiptsAPI
import no.nav.altinn.admin.service.srr.AltinnSRRService
import no.nav.altinn.admin.service.srr.ssrAPI
import no.nav.altinn.admin.ws.Clients
import org.slf4j.event.Level

const val AUTHENTICATION_BASIC = "basicAuth"
private val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

val swagger = Swagger(
    info = Information(
        version = System.getenv("APP_VERSION")?.toString() ?: "",
        title = "Altinn Admin API",
        description = "[altinn-admin](https://github.com/navikt/altinn-admin)",
        contact = Contact(
            name = "Slack #Team_Altinn",
            url = "https://github.com/navikt/altinn-admin",
            email = "nav.altinn.lokalforvaltning@nav.no"
        )
    )
)

private val logger = KotlinLogging.logger { }

internal const val SWAGGER_URL_V1 = "$API_V1/apidocs/index.html?url=swagger.json"

@KtorExperimentalLocationsAPI
fun main() = bootstrap(ApplicationState(), Environment())

@KtorExperimentalLocationsAPI
fun bootstrap(applicationState: ApplicationState, environment: Environment) {
    val applicationServer = embeddedServer(
        Netty, environment.application.port, module = { mainModule(environment, applicationState) }
    )
    applicationState.initialized = true
    logger.info { "Application ready" }

    DefaultExports.initialize()
    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info { "Shutdown hook called, shutting down gracefully" }
            applicationState.initialized = false
            applicationState.running = false
            applicationServer.stop(1000, 5000)
        }
    )
    applicationServer.start(wait = true)
}

@KtorExperimentalLocationsAPI
fun Application.mainModule(environment: Environment, applicationState: ApplicationState) {
    logger.info { "Starting server" }
    System.setProperty("javax.xml.soap.SAAJMetaFactory", "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl")

    install(Sessions) {
        cookie<UserSession>("user_session")
    }

    install(DefaultHeaders) {
        header(HttpHeaders.CacheControl, CacheControl.NO_CACHE)
    }
    install(ConditionalHeaders)
    install(Compression)
    install(AutoHeadResponse)
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith(API_V1) }
        filter { call -> call.request.path().startsWith(API_V2) }
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            logger.error(cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
//    install(Authentication) {
//        basic(name = AUTHENTICATION_BASIC) {
//            realm = "altinn-admin"
//            validate { credentials ->
//                LDAPAuthenticate(environment.application).use { ldap ->
//                    if (ldap.canUserAuthenticate(credentials.name, credentials.password))
//                        UserIdPrincipal(credentials.name)
//                    else
//                        null
//                }
//            }
//        }
//    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
        register(ContentType.Application.Xml, JacksonConverter(objectMapper))
    }

//    val wellKnownInternalAzureAd = getWellKnown(
//        wellKnownUrl = environment.azure.azureAppWellKnownUrl
//    )
//    installJwtAuthentication(
//        jwtIssuerList = listOf(
//            JwtIssuer(
//                acceptedAudienceList = listOf(environment.azure.azureAppClientId),
//                jwtIssuerType = JwtIssuerType.INTERNAL_AZUREAD,
//                wellKnown = wellKnownInternalAzureAd,
//            ),
//        ),
//    )
    val httpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
    install(Authentication) {
        oauth("auth-oauth-microsoft") {
            urlProvider = { "https://altinn-admin.dev.intern.nav.no/oauth2/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "microsoft",
                    authorizeUrl = "https://login.microsoftonline.com/common/oauth2/authorize",
                    accessTokenUrl = "https://login.microsoftonline.com/common/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = environment.azure.azureAppClientId,
                    clientSecret = environment.azure.azureAppClientSecret,
                    defaultScopes = listOf("openid"),
                )
            }
            client = httpClient
        }
    }
    install(Locations)
    install(CallId) {
        generate { randomUuid() }
        verify { callId: String -> callId.isNotEmpty() }
        header(HttpHeaders.XCorrelationId)
    }

    val swaggerUI = SwaggerUi()

    val altinnSRRService = AltinnSRRService(environment) {
        Clients.iRegisterSRRAgencyExternalBasic(environment.altinn.altinnSrrUrl)
    }
    val altinnDqService = AltinnDQService(environment) {
        Clients.iDownloadQueueExternalBasic(environment.altinn.altinnDqUrl)
    }
    val altinnCorrespondenceService = AltinnCorrespondenceService(environment) {
        Clients.iCorrespondenceExternalBasic(environment.altinn.altinnCorrespondenceUrl)
    }
    val altinnPrefillService = AltinnPrefillService(environment) {
        Clients.iPrefillExternalBasic(environment.altinn.altinnPrefillUrl)
    }
    val altinnReceiptService = AltinnReceiptService(environment) {
        Clients.iReceiptAgencyExternalBasic(environment.altinn.altinnReceiptUrl)
    }

    val expireAlerts = ExpireAlerts(environment, applicationState, altinnSRRService)
    if (!environment.application.devProfile) {
        launch(backgroundTasksContext) {
            expireAlerts.checkDates()
        }
    }
    var maskinporten: MaskinportenClient? = null
    if (environment.application.localEnv == "preprod") {
        maskinporten = MaskinportenClient(environment)
//        runBlocking {
//            val test = maskinporten.tokenRequest()
//            logger.info { "Got a token from maskinporten: $test" }
//        }
    }

    logger.info { "Installing routes" }
    install(Routing) {
        get("/") { call.respondRedirect(SWAGGER_URL_V1) }
        get("/api") { call.respondRedirect(SWAGGER_URL_V1) }
        get(API_V1) { call.respondRedirect(SWAGGER_URL_V1) }
        get("$API_V1/apidocs") { call.respondRedirect(SWAGGER_URL_V1) }
        get("$API_V1/apidocs/{fileName}") {
            val fileName = call.parameters["fileName"]
            if (fileName == "swagger.json") call.respond(swagger) else swaggerUI.serve(fileName, call)
        }
//        authenticate("auth-oauth-microsoft") {
//            get("/oauth2/login") {}
//            get("/oauth2/callback") {
//                val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
//                logger.debug { "access token: ${principal?.accessToken}" }
//                call.sessions.set(UserSession(principal?.accessToken.toString()))
//                call.respondRedirect(SWAGGER_URL_V1)
//            }
//        }
        get("/hello") {
            val userSession: UserSession? = call.sessions.get<UserSession>()
            if (userSession != null) {
                val userInfo: UserInfo = httpClient.get("https://login.microsoftonline.com/common/openid/userinfo") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${userSession.token}")
                    }
                }
                call.respondText("Hello, ${userInfo.name}!")
            } else {
                call.respondRedirect("/")
            }
        }

        logger.info { "Installing altinn srr api" }
        ssrAPI(altinnSrrService = altinnSRRService, environment = environment)
        logger.info { "Installing altinn dq api" }
        dqAPI(altinnDqService = altinnDqService, environment = environment)
        logger.info { "Installing altinn correspondence api" }
        correspondenceAPI(altinnCorrespondenceService = altinnCorrespondenceService, environment = environment)
        logger.info { "Installing altinn receipts api" }
        prefillAPI(altinnPrefillService = altinnPrefillService, environment = environment)
        receiptsAPI(altinnReceiptService = altinnReceiptService, environment = environment)
        nais(readinessCheck = { applicationState.initialized }, livenessCheck = { applicationState.running })
        loginAPI(environment = environment)
        if (environment.application.localEnv == "preprod" && maskinporten != null) {
            logger.info { "Installing routes for altinn/api/serviceowner/" }
            ownerApi(maskinporten, environment)
        }
    }
}

package no.nav.altinn.admin.service.srr

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import no.altinn.schemas.services.register.srr._2015._06.AddRightResponse
import no.altinn.schemas.services.register.srr._2015._06.AddRightResponseList
import no.altinn.schemas.services.register.srr._2015._06.DeleteRightResponse
import no.altinn.schemas.services.register.srr._2015._06.DeleteRightResponseList
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.common.xmlMapper
import no.nav.altinn.admin.generateJWT
import no.nav.altinn.admin.installAuthentication
import no.nav.altinn.admin.installCommon
import org.amshove.kluent.shouldBeEqualTo

@KtorExperimentalLocationsAPI
class AltinnSRRServiceTest {
    val applicationState = ApplicationState(running = true, initialized = true)
    val engine = TestApplicationEngine(createTestEnvironment())
    val testEnvironment = Environment()

    @BeforeTest
    fun beforeTest() {
        engine.start(wait = false)
        val httpClient = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = JacksonSerializer { objectMapper }
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.NONE
            }
        }
        val issuerUrl = "https://sts.issuer.net/myid"

        val path = "src/test/resources/jwkset.json"
        val uri = Paths.get(path).toUri().toURL()
        val jwkProvider = JwkProviderBuilder(uri).build()
        val acceptedClientId = testEnvironment.azure.azureAppClientId
        engine.application.installAuthentication(
            testEnvironment,
            httpClient,
            jwkProvider,
            issuerUrl,
            acceptedClientId,
            "https://wellknown.config"
        )
        engine.application.installCommon(testEnvironment, applicationState, httpClient)
    }

    @Test
    fun `Legg til rettighet med tomt virksomhetsnummer`() {
        with(engine) {
            val test = objectMapper.writeValueAsString(
                PostLeggTilRettighetBody(
                    "1234",
                    "1",
                    "",
                    RettighetType.Les,
                    "*.nav.no",
                    null
                )
            )
            val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Legg til rettighet med ugyldig utgaveKode`() {
        with(engine) {
            val test = objectMapper.writeValueAsString(
                PostLeggTilRettighetBody(
                    "1234",
                    "T",
                    "123123123",
                    RettighetType.Les,
                    "*.nav.no",
                    null
                )
            )
            val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Legg til rettighet med ugyldig tjenesteKode`() {
        with(engine) {
            val test = objectMapper.writeValueAsString(
                PostLeggTilRettighetBody(
                    "4252",
                    "1",
                    "123123123",
                    RettighetType.Les,
                    "*.nav.no",
                    null
                )
            )
            val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Legg til rettighet med tomt domene`() {
        with(engine) {
            val test = objectMapper.writeValueAsString(
                PostLeggTilRettighetBody(
                    "1234",
                    "1",
                    "123123123",
                    RettighetType.Les,
                    "",
                    null
                )
            )
            val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Legg til rettighet som er gyldig`() {
        with(engine) {
            val test = objectMapper.writeValueAsString(
                PostLeggTilRettighetBody(
                    "1234",
                    "1",
                    "123123123",
                    RettighetType.Les,
                    "*.nav.no",
                    null
                )
            )
            val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.OK
        }
    }

    @Test
    fun `Legg til rettighet som allerede eksisterer`() {
        with(engine) {
            testEnvironment.mock.srrAddResponse = AddRightResponseList().apply {
                addRightResponse.add(
                    xmlMapper.readValue(
                        "<AddRightResponse>\n" +
                            "       <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
                            "        <Reportee>958995369</Reportee>\n" +
                            "        <Right>Read</Right>\n" +
                            "        <ValidTo>2020-12-03T00:00:00</ValidTo>\n" +
                            "        <OperationResult>RULE_ALREADY_EXISTS</OperationResult>\n" +
                            "        </AddRightResponse>\n",
                        AddRightResponse::class.java
                    )
                )
            }
            val test = objectMapper.writeValueAsString(
                PostLeggTilRettighetBody(
                    "1234",
                    "1",
                    "123123123",
                    RettighetType.Les,
                    "*.nav.no",
                    null
                )
            )

            val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Legg til rettighet for virksomhet som ikke finnes i ER`() {
        with(engine) {
            testEnvironment.mock.srrAddResponse = AddRightResponseList().apply {
                addRightResponse.add(
                    xmlMapper.readValue(
                        "<AddRightResponse>\n" +
                            "    <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
                            "    <Reportee>958995367</Reportee>\n" +
                            "    <Right>Read</Right>\n" +
                            "    <ValidTo>2020-12-03T00:00:00</ValidTo>\n" +
                            "    <OperationResult>EMPTY_OR_NOT_A_VALID_SSN_OR_ORGANISATION</OperationResult>\n" +
                            "    </AddRightResponse>\n",
                        AddRightResponse::class.java
                    )
                )
            }
            val test = objectMapper.writeValueAsString(
                PostLeggTilRettighetBody(
                    "1234",
                    "1",
                    "123123123",
                    RettighetType.Les,
                    "*.nav.no",
                    null
                )
            )

            val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Legg til rettighet for virksomhet med en dato tilbake i tid`() {
        with(engine) {
            testEnvironment.mock.srrAddResponse = AddRightResponseList().apply {
                addRightResponse.add(
                    xmlMapper.readValue(
                        "<AddRightResponse>\n" +
                            "    <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
                            "    <Reportee>958995367</Reportee>\n" +
                            "    <Right>Read</Right>\n" +
                            "    <ValidTo>2018-12-03T00:00:00</ValidTo>\n" +
                            "    <OperationResult>Right_Already_Expired</OperationResult>\n" +
                            "    </AddRightResponse>\n",
                        AddRightResponse::class.java
                    )
                )
            }
            val test = objectMapper.writeValueAsString(
                PostLeggTilRettighetBody(
                    "1234",
                    "1",
                    "123123123",
                    RettighetType.Les,
                    "*.nav.no",
                    null
                )
            )

            val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Legg til rettighet feiler pga ukjent årsak`() {
        with(engine) {
            testEnvironment.mock.srrAddResponse = AddRightResponseList().apply {
                addRightResponse.add(
                    xmlMapper.readValue(
                        "<AddRightResponse>\n" +
                            "    <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
                            "    <Reportee>958995367</Reportee>\n" +
                            "    <Right>Read</Right>\n" +
                            "    <ValidTo>2018-12-03T00:00:00</ValidTo>\n" +
                            "    <OperationResult>Unknown</OperationResult>\n" +
                            "    </AddRightResponse>\n",
                        AddRightResponse::class.java
                    )
                )
            }
            val test = objectMapper.writeValueAsString(
                PostLeggTilRettighetBody(
                    "1234",
                    "1",
                    "123123123",
                    RettighetType.Les,
                    "*.nav.no",
                    null
                )
            )

            val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Slett rettighet med ugyldig tjeneste`() {
        with(engine) {
            val tjeneste = "Samtykke_UFORE"
            val orgnr = "123123123"
            val lesEllerSkriv = RettighetType.Les
            val domene = "*.nav.no"
            val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
            val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
            }
            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Slett rettighet med ugyldig virksomhetsnummer`() {
        with(engine) {
            val tjeneste = "Samtykke_AAP"
            val orgnr = "123"
            val lesEllerSkriv = RettighetType.Les
            val domene = "*.nav.no"
            val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
            val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
            }
            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Slett rettighet med ugyldig RettighetType`() {
        with(engine) {
            val tjeneste = "Samtykke_AAP"
            val orgnr = "123123123"
            val lesEllerSkriv = "dust"
            val domene = "*.nav.no"
            val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
            val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
                addHeader(HttpHeaders.Authorization, "Bearer 1234567890")
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
            }
            req.response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
        }
    }

    @Test
    fun `Slett rettighet med tomt domene`() {
        with(engine) {
            val tjeneste = "Samtykke_AAP"
            val orgnr = "123123123"
            val lesEllerSkriv = RettighetType.Les
            val domene = " "
            val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
            val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
            }
            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Slett rettighet med alle parametere gyldig`() {
        with(engine) {
            val tjeneste = "Samtykke_AAP"
            val orgnr = "123123123"
            val lesEllerSkriv = RettighetType.Les
            val domene = "*.nav.no"
            val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
            val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
            }
            req.response.status() shouldBeEqualTo HttpStatusCode.OK
        }
    }

    @Test
    fun `Slett rettighet med ukjent regel`() {
        with(engine) {
            testEnvironment.mock.srrDeleteResponse = DeleteRightResponseList().apply {
                deleteRightResponse.add(
                    xmlMapper.readValue(
                        "<DeleteRightResponse>\n" +
                            "    <Condition>AllowedRedirectDomain:*.tull.altinn.no;*.test.altinn.no</Condition>\n" +
                            "    <Reportee>958995369</Reportee>\n" +
                            "    <Right>Read</Right>\n" +
                            "    <OperationResult>Rule_Not_Found</OperationResult>\n" +
                            "    </DeleteRightResponse>",
                        DeleteRightResponse::class.java
                    )
                )
            }
            val tjeneste = "Samtykke_AAP"
            val orgnr = "123123123"
            val lesEllerSkriv = RettighetType.Les
            val domene = "*.nav.no"
            val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
            val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
                addHeader(
                    HttpHeaders.Authorization,
                    "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                )
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
            }
            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Hent rettigheter for virksomhetsnummer 123`() {
        with(engine) {
            with(
                handleRequest(
                    HttpMethod.Get,
                    "/api/v1/altinn/rettighetsregister/hent/tjenester/org/Samtykke_AAP/123"
                ) {
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                    )
                }
            ) {
                response.status() shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `Hent rettigheter for en virksomhet`() {
        with(engine) {
            with(
                handleRequest(
                    HttpMethod.Get,
                    "/api/v1/altinn/rettighetsregister/hent/tjenester/org/Samtykke_AAP/123123123"
                ) {
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                    )
                }
            ) {
                response.status() shouldBeEqualTo HttpStatusCode.OK
            }
        }
    }

    @Test
    fun `Hent rettigheter for alle virksomheter`() {
        with(engine) {
            with(
                handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent/tjenester/Samtykke_AAP") {
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                    )
                    addHeader(HttpHeaders.Accept, "application/json")
                    addHeader("Content-Type", "application/json")
                }
            ) {
                response.status() shouldBeEqualTo HttpStatusCode.OK
            }
        }
    }

    @Test
    fun `Hent rettigheter på UFORE for alle virksomheter`() {
        with(engine) {
            with(
                handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent/tjeneste/Samtykke_UFORE") {
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                    )
                }
            ) {
                response.status() shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `Hent rettigheter på AAP for alle virksomheter`() {
        with(engine) {
            with(
                handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent/tjeneste/Samtykke_AAP") {
                    addHeader(
                        HttpHeaders.Authorization,
                        "Bearer ${generateJWT("2", testEnvironment.azure.azureAppClientId)}"
                    )
                }
            ) {
                response.status() shouldBeEqualTo HttpStatusCode.OK
            }
        }
    }

    @AfterTest
    fun afterTest() {
    }

//                    it("ServiceAccounts har aksess til slett rettighet med alle param gyldig skal feile 'bad request'") {
//                        val tjeneste = "Samtykke_AAP"
//                        val orgnr = "123123123"
//                        val lesEllerSkriv = RettighetType.Les
//                        val domene = "*.nav.no"
//                        val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
//                        val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("srvp01:dummy".toByteArray())}")
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
}

package no.nav.altinn.admin.service.correpondence

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
import java.nio.file.Paths
import kotlin.test.BeforeTest
import kotlin.test.Test
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.encodeBase64
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.installAuthentication
import no.nav.altinn.admin.installCommon
import org.amshove.kluent.shouldBeEqualTo

@KtorExperimentalLocationsAPI
class AltinnCorrespondenceServiceTest {
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
        val consumerClientId = "1"
        val acceptedClientId = "2"
        val notAcceptedClientId = "4"
        engine.application.installAuthentication(testEnvironment, httpClient, jwkProvider, issuerUrl, acceptedClientId)
        engine.application.installCommon(testEnvironment, applicationState, httpClient)
    }

    @Test
    fun `Hent meldingsstatuser fra en meldingstjeneste med ugyldig tjenestekode`() {
        with(engine) {
            val params = "Faktura"
            val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$params") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Bearer 1234567890")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Hent meldingsstatuser fra en meldingstjeneste med gyldig tjenestekode men feil tom dato`() {
        with(engine) {
            val sc = "K27"
            val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$sc/2020-01-01/2020-0706") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Hent meldingsstatuser fra en meldingstjeneste med gyldig tjenestekode, feil fom dato`() {
        with(engine) {
            val sc = "K27"
            val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$sc/01-01-2020/2020-07-06/1") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Hent meldingsstatuser fra en meldingstjeneste med gyldig tjenestekode, feil mottaker id`() {
        with(engine) {
            val sc = "K27"
            val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$sc/2020-01-01/2020-07-06/1") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Hent meldingsstatuser fra en meldingstjeneste v2 med ugyldig utgavekode`() {
        with(engine) {
            val params = "4626/tull"
            val req = handleRequest(HttpMethod.Get, "/api/v2/altinn/meldinger/hent/$params") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000002:itest2".toByteArray())}")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Hent meldingsstatuser fra en meldingstjeneste v2 med gyldig tjenestekode, men feil tom dato`() {
        with(engine) {
            val sc = "4626"
            val req = handleRequest(HttpMethod.Get, "/api/v2/altinn/meldinger/hent/$sc/1/2020-01-01/2020-0706") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Hent meldingsstatuser fra en meldingstjeneste 4503 v2 med gyldig tjenestekode, men feil tom dato`() {
        with(engine) {
            val sc = "4503"
            val req = handleRequest(HttpMethod.Get, "/api/v2/altinn/meldinger/hent/$sc/1/2020-01-01/2020-0706") {
                addHeader(HttpHeaders.Accept, "application/json")
                addHeader("Content-Type", "application/json")
                addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
            }

            req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
        }
    }
//                context("Route /api/v1/altinn/meldinger/send") {
//                    it("Send melding til en gyldig meldingstjeneste") {
//                        val test = objectMapper.writeValueAsString(PostCorrespondenceBody("4626", "1", "910521594",
//                        Melding("En enkel melding, tittel her", "Et kort sammendrag her!", "Selve kropp melding!"), null, null))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/meldinger/send") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//
//                        req.requestHandled shouldEqual true
//                        req.response.status() shouldEqual HttpStatusCode.OK
//                    }
//                }
//                context("Route /api/v1/altinn/meldinger/send vedlegg") {
//                    it("Send melding til en gyldig melding med vedlegg") {
//                        val vedlegger = mutableListOf<Vedlegg>()
//                        val file = "src/test/resources/ImplementasjonsguideForSluttbrukersystemer.pdf"
//                        vedlegger.add(Vedlegg("ImplementasjonsguideForSluttbrukersystemer.pdf", "Implementasjonsguide for sluttbrukersystemer", File(file).readBytes()))
//                        val test = objectMapper.writeValueAsString(PostCorrespondenceBody("4626", "1", "910521594",
//                            Melding("En melding med vedlegg, tittel her", "Et kort sammendrag her!", "Selve kropp melding!"), null,
//                        vedlegger))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/meldinger/send") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//
//                        req.requestHandled shouldEqual true
//                        req.response.status() shouldEqual HttpStatusCode.OK
//                    }
//                }
//                context("Route /api/v1/altinn/meldinger/send") {
//                    it("Send melding med varsler til en gyldig meldingstjeneste") {
//                        val varsler = mutableListOf<Varsel>()
//                        val ekstraMottaker = mutableListOf<Mottaker>()
//                        ekstraMottaker.add(Mottaker(ForsendelseType.Email, "osengr@gmail.com"))
//                        varsler.add(Varsel("ikke-besvar-denne@nav.no", LocalDateTime.now().plusMinutes(10), VarselType.TokenTextOnly, "Tittel i varsel", "Innhold i varsel", ekstraMottaker))
//                        val test = objectMapper.writeValueAsString(PostCorrespondenceBody("4626", "1", "910521594",
//                        Melding("En enkel melding, tittel her", "Et kort sammendrag her!", "Selve kropp melding!"),
//                            varsler, null))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/meldinger/send") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//
//                        req.requestHandled shouldEqual true
//                        req.response.status() shouldEqual HttpStatusCode.OK
//                    }
//                }
//    describe("TEST only stub") {
//        val soapServer = WireMockServer(options().dynamicPort().notifier(Slf4jNotifier(true)))
//            .also { it.start() }
//
//        context("Route /api/v1/altinn/meldinger/hent/{tjenesteKode}/{fom}/{tom}/{mottaker}") {
//            soapServer.apply {
//                stubFor(get(urlEqualTo("/api/v1/altinn/meldinger/hent/4626/2020-01-01/2020-07-06/123123123"))
//                    .atPriority(1)
//                    .withBasicAuth("n000001", "itest1")
//                    .willReturn(aResponse()
//                        .withStatus(HttpStatus.OK_200)
//                        .withHeader(HttpHeaders.ContentType, ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8).toString())
//                        .withBody("""
//                    {
//                      "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzcnZhbHRpbm5rYW5hbCIsIm5hbWUiOiJKb2huIERvZSIsImFkbWluIjp0cnVlLCJpYXQiOjE1MTYyMzkwMjJ9.aNXjCqFzVlBvQscdNU8oyX2JgByAoXFWHWlNe_JJ8-bTJ16ls5bW1ICJ6CMAbC68-0jxlGtgQaZjC_pOtrkZPQvTkoEN0OKl3mPkR4PxgHAXP2KiH8HExFB2xjGetlOB_1-EuK0uMAuhRgeeFKCPz5AWNIcveTBY4nYlki3Ajuo",
//                      "token_type": "Bearer",
//                      "expires_in": 3600
//                    }
//                """.trimIndent())
//                    )
//                )
//            }
//
//            it("Hent meldingsstatuser fra en meldingstjeneste med gyldig tjenestekode, feil mottaker id") {
//                val sc = "4626"
//                val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$sc/2020-01-01/2020-07-06/123123123") {
//                    addHeader(HttpHeaders.Accept, "application/json")
//                    addHeader("Content-Type", "application/json")
//                    addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                }
//
//                req.requestHandled shouldEqual true
//                req.response.status() shouldEqual HttpStatusCode.OK
//            }
//        }
//    }
}

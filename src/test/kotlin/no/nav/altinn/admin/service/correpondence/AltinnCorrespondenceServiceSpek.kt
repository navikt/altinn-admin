package no.nav.altinn.admin.service.correpondence

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.InMemoryLDAPServer
import no.nav.altinn.admin.common.encodeBase64
import no.nav.altinn.admin.mainModule
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
object AltinnCorrespondenceServiceSpek : Spek({
    val applicationState = ApplicationState(running = true, initialized = true)

    describe("Test all authorized calls") {
        context("Get Route /api/v1/altinn/meldinger/hent/{tjenesteKode}") {
            val engine = TestApplicationEngine(createTestEnvironment())
            val testEnvironment = Environment()

            beforeGroup {
                InMemoryLDAPServer.start()
                engine.start(wait = false)
                engine.application.mainModule(testEnvironment, applicationState = applicationState)
            }
            with(engine) {
                context("Route /api/v1/altinn/meldinger/hent/{tjeneste}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste med ugyldig tjenestekode") {
                        val params = "Faktura"
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$params") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
                context("Route /api/v1/altinn/meldinger/hent/{tjeneste}/{fom}/{tom}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste med gyldig tjenestekode, men feil tom dato") {
                        val sc = "K27"
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$sc/2020-01-01/2020-0706") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
                context("Route /api/v1/altinn/meldinger/hent/{tjenesteKode}/{fom}/{tom}/{mottaker}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste med gyldig tjenestekode, feil fom dato") {
                        val sc = "K27"
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$sc/01-01-2020/2020-07-06/1") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
                context("Route /api/v1/altinn/meldinger/hent/{tjeneste}/{fom}/{tom}/{mottaker}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste med gyldig tjenestekode, feil mottaker id") {
                        val sc = "K27"
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$sc/2020-01-01/2020-07-06/1") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
                context("Route /api/v2/altinn/meldinger/hent/{tjenesteKode}/{utgaveKode}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste v2 med ugyldig utgavekode") {
                        val params = "4626/tull"
                        val req = handleRequest(HttpMethod.Get, "/api/v2/altinn/meldinger/hent/$params") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000002:itest2".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
                context("Route /api/v2/altinn/meldinger/hent/{tjenesteKode}/{utgaveKode}/{fom}/{tom}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste v2 med gyldig tjenestekode, men feil tom dato") {
                        val sc = "4626"
                        val req = handleRequest(HttpMethod.Get, "/api/v2/altinn/meldinger/hent/$sc/1/2020-01-01/2020-0706") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
                context("Route /api/v2/altinn/meldinger/hent/{tjenesteKode}/{utgaveKode}/{fom}/{tom}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste v2 med gyldig tjenestekode, men feil tom dato") {
                        val sc = "4503"
                        val req = handleRequest(HttpMethod.Get, "/api/v2/altinn/meldinger/hent/$sc/1/2020-01-01/2020-0706") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
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
            }
            afterGroup {
                InMemoryLDAPServer.stop()
            }
        }
    }
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
})

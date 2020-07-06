package no.nav.altinn.admin.service.correpondence

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
// import io.ktor.server.testing.setBody
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.InMemoryLDAPServer
import no.nav.altinn.admin.common.encodeBase64
// import no.nav.altinn.admin.common.objectMapper
// import no.nav.altinn.admin.service.correspondence.PostCorrespondenceBody
import no.nav.altinn.admin.mainModule
// import no.nav.altinn.admin.service.correspondence.ForsendelseType
// import no.nav.altinn.admin.service.correspondence.Melding
// import no.nav.altinn.admin.service.correspondence.Mottaker
// import no.nav.altinn.admin.service.correspondence.Varsel
// import no.nav.altinn.admin.service.correspondence.VarselType
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
// import java.time.LocalDateTime

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
                context("Route /api/v1/altinn/meldinger/hent/{tjenesteKode}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste med ugyldig tjenestekode") {
                        val params = "4826"
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$params") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
                context("Route /api/v1/altinn/meldinger/hent/{tjenesteKode}/{fom}/{tom}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste med gyldig tjenestekode, men feil tom dato") {
                        val sc = "4626"
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
                        val sc = "4626"
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$sc/01-01-2020/2020-07-06/1") {
                            addHeader(HttpHeaders.Accept, "application/json")
                            addHeader("Content-Type", "application/json")
                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
                        }

                        req.requestHandled shouldEqual true
                        req.response.status() shouldEqual HttpStatusCode.BadRequest
                    }
                }
                context("Route /api/v1/altinn/meldinger/hent/{tjenesteKode}/{fom}/{tom}/{mottaker}") {
                    it("Hent meldingsstatuser fra en meldingstjeneste med gyldig tjenestekode, feil mottaker id") {
                        val sc = "4626"
                        val req = handleRequest(HttpMethod.Get, "/api/v1/altinn/meldinger/hent/$sc/2020-01-01/2020-07-06/1") {
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
})

/*
package no.nav.altinn.admin.service.srr

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.ApplicationState
import no.nav.altinn.admin.common.objectMapper
import no.nav.altinn.admin.mainModule
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object AltinnSRRServiceSpek : Spek({
    val applicationState = ApplicationState(running = true, initialized = true)

    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("POST Legg til et ugyldig virksomhetsnummer.") {
            val test = objectMapper.writeValueAsString(RequestRegister("", "*.nav.no", "read"))
            val req = handleRequest {
                method = HttpMethod.Post
                uri = "/api/v1/altinn/rettighetsregister/leggtil"
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.requestHandled shouldEqual true
            it("should fail, due to empty virksomhetsnummer") {
                req.response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("POST Legg til med ok respons.") {
            val test = objectMapper.writeValueAsString(RequestRegister("123123123", "*.nav.no", "read"))
            val req = handleRequest {
                method = HttpMethod.Post
                uri = "/api/v1/altinn/rettighetsregister/leggtil"
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.requestHandled shouldEqual true
            it("should be ok, a valid virksomhetsnummer") {
                req.response.status() shouldEqual HttpStatusCode.OK
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(mock = Environment.Mock(srrAddXmlResponse = "<AddRightResponse>\n" +
            "               <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
            "               <Reportee>958995369</Reportee>\n" +
            "               <Right>Read</Right>\n" +
            "               <ValidTo>2020-12-03T00:00:00</ValidTo>\n" +
            "               <OperationResult>RULE_ALREADY_EXISTS</OperationResult>\n" +
            "            </AddRightResponse>\n")), applicationState) }) {
        describe("POST Legg til en regel som allerede eksisterer.") {
            val test = objectMapper.writeValueAsString(RequestRegister("123123123", "*.nav.no", "read"))
            val req = handleRequest {
                method = HttpMethod.Post
                uri = "/api/v1/altinn/rettighetsregister/leggtil"
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.requestHandled shouldEqual true
            it("should fail, due to rule already exist") {
                req.response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(mock = Environment.Mock(srrAddXmlResponse = "<AddRightResponse>\n" +
            "    <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
            "    <Reportee>958995367</Reportee>\n" +
            "    <Right>Read</Right>\n" +
            "    <ValidTo>2020-12-03T00:00:00</ValidTo>\n" +
            "    <OperationResult>EMPTY_OR_NOT_A_VALID_SSN_OR_ORGANISATION</OperationResult>\n" +
            "    </AddRightResponse>\n")), applicationState) }) {
        describe("POST Legg til en med ugyldig virksomhetsnummer, finnes ikke i altinns ER.") {
            val test = objectMapper.writeValueAsString(RequestRegister("123123123", "*.nav.no", "read"))
            val req = handleRequest {
                method = HttpMethod.Post
                uri = "/api/v1/altinn/rettighetsregister/leggtil"
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.requestHandled shouldEqual true
            it("should fail, due to invalid number") {
                req.response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(mock = Environment.Mock(srrAddXmlResponse = "<AddRightResponse>\n" +
            "    <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
            "    <Reportee>958995367</Reportee>\n" +
            "    <Right>Read</Right>\n" +
            "    <ValidTo>2018-12-03T00:00:00</ValidTo>\n" +
            "    <OperationResult>Right_Already_Expired</OperationResult>\n" +
            "    </AddRightResponse>\n")), applicationState) }) {
        describe("POST Legg til med feil dato, tilbake i tid.") {
            val test = objectMapper.writeValueAsString(RequestRegister("123123123", "*.nav.no", "read"))
            val req = handleRequest {
                method = HttpMethod.Post
                uri = "/api/v1/altinn/rettighetsregister/leggtil"
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.requestHandled shouldEqual true
            it("should fail, due to date already expired") {
                req.response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(mock = Environment.Mock(srrAddXmlResponse = "<AddRightResponse>\n" +
            "    <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
            "    <Reportee>958995367</Reportee>\n" +
            "    <Right>Read</Right>\n" +
            "    <ValidTo>2018-12-03T00:00:00</ValidTo>\n" +
            "    <OperationResult>Unknown</OperationResult>\n" +
            "    </AddRightResponse>\n")), applicationState) }) {
        describe("POST Legg til med en ukjent feil respons.") {
            val test = objectMapper.writeValueAsString(RequestRegister("123123123", "*.nav.no", "read"))
            val req = handleRequest {
                method = HttpMethod.Post
                uri = "/api/v1/altinn/rettighetsregister/leggtil"
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.requestHandled shouldEqual true
            it("should fail, due to unknown error") {
                req.response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("POST Fjern et ugyldig virksomhetsnummer.") {
            val mapper = jacksonObjectMapper()
            val test = mapper.writeValueAsString(RequestRegister("", "*.nav.no", "read"))
            val req = handleRequest {
                method = HttpMethod.Post
                uri = "/api/v1/altinn/rettighetsregister/fjern"
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.requestHandled shouldEqual true
            it("should fail, due to empty virksomhetsnummer") {
                req.response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("POST Fjern med gyldig virksomhetsnummer.") {
            val mapper = jacksonObjectMapper()
            val test = mapper.writeValueAsString(RequestRegister("123123123", "*.nav.no", "read"))
            val req = handleRequest {
                method = HttpMethod.Post
                uri = "/api/v1/altinn/rettighetsregister/fjern"
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.requestHandled shouldEqual true
            it("should be ok, due to valid virksomhetsnummer") {
                req.response.status() shouldEqual HttpStatusCode.OK
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(mock = Environment.Mock(srrDeleteXmlResponse = "<DeleteRightResponse>\n" +
            "    <Condition>AllowedRedirectDomain:*.tull.altinn.no;*.test.altinn.no</Condition>\n" +
            "    <Reportee>958995369</Reportee>\n" +
            "    <Right>Read</Right>\n" +
            "    <OperationResult>Rule_Not_Found</OperationResult>\n" +
            "    </DeleteRightResponse>")), applicationState) }) {
        describe("POST Fjern en regel som ikke finnes i registeret.") {
            val mapper = jacksonObjectMapper()
            val test = mapper.writeValueAsString(RequestRegister("123123123", "*.nav.no", "read"))
            val req = handleRequest {
                method = HttpMethod.Post
                uri = "/api/v1/altinn/rettighetsregister/fjern"
                addHeader("Content-Type", "application/json")
                setBody(test)
            }

            req.requestHandled shouldEqual true
            it("should fail, due to invalid condition.") {
                req.response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("GET Hent rettigheter for et virksomhetsnummer.") {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent?orgnr=123")) {
                it("should fail with bad request") {
                    response.status() shouldEqual HttpStatusCode.BadRequest
                }
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("GET Hent rettigheter for alle virksomheter.") {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent")) {
                it("should be ok") {
                    response.status() shouldEqual HttpStatusCode.OK
                }
            }
        }
    }

    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
        describe("GET Hent rettigheter for en virksomhet.") {
            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent?orgnr=123123123")) {
                it("should be ok") {
                    response.status() shouldEqual HttpStatusCode.OK
                }
            }
        }
    }
})

 */
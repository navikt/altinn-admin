//package no.nav.altinn.admin.service.srr
//
//import io.ktor.http.HttpHeaders
//import io.ktor.http.HttpMethod
//import io.ktor.http.HttpStatusCode
//import io.ktor.locations.KtorExperimentalLocationsAPI
//import io.ktor.server.testing.TestApplicationEngine
//import io.ktor.server.testing.createTestEnvironment
//import io.ktor.server.testing.handleRequest
//import io.ktor.server.testing.setBody
//import io.ktor.server.testing.withTestApplication
//import no.altinn.schemas.services.register.srr._2015._06.AddRightResponse
//import no.altinn.schemas.services.register.srr._2015._06.AddRightResponseList
//import no.altinn.schemas.services.register.srr._2015._06.DeleteRightResponse
//import no.altinn.schemas.services.register.srr._2015._06.DeleteRightResponseList
//import no.nav.altinn.admin.Environment
//import no.nav.altinn.admin.common.ApplicationState
//import no.nav.altinn.admin.common.InMemoryLDAPServer
//import no.nav.altinn.admin.common.encodeBase64
//import no.nav.altinn.admin.common.objectMapper
//import no.nav.altinn.admin.common.xmlMapper
//import no.nav.altinn.admin.mainModule
//import org.amshove.kluent.shouldBeEqualTo
//import org.spekframework.spek2.Spek
//import org.spekframework.spek2.style.specification.describe
//
//@KtorExperimentalLocationsAPI
//object AltinnSRRServiceSpek : Spek({
//    val applicationState = ApplicationState(running = true, initialized = true)
//
//    describe("Test all authorized calls") {
//        context("POST Route /api/v1/altinn/rettighetsregister/leggtil") {
//            val engine = TestApplicationEngine(createTestEnvironment())
//            val testEnvironment = Environment()
//            beforeGroup {
//                InMemoryLDAPServer.start()
//                engine.start(wait = false)
//                engine.application.mainModule(testEnvironment, applicationState = applicationState)
//            }
//            with(engine) {
//                context("Route /api/v1/altinn/rettighetsregister/leggtil") {
//                    it("Legg til rettighet med tomt virksomhetsnummer skal feile med 'bad request'") {
//                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("1234", "1", "", RettighetType.Les, "*.nav.no"))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                    it("Legg til rettighet med ugyldig tjenesteKode skal feile med 'bad request'") {
//                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("4252", "1", "123123123", RettighetType.Les, "*.nav.no"))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                    it("Legg til rettighet med ugyldig utgaveKode skal feile med 'bad request'") {
//                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("1234", "T", "123123123", RettighetType.Les, "*.nav.no"))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
////                    it("Legg til rettighet med feil lesEllerSkriv skal feile med 'bad request'") {
////                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("1234", "1", "123123123", "dust", "*.nav.no"))
////                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
////                            addHeader(HttpHeaders.Accept, "application/json")
////                            addHeader("Content-Type", "application/json")
////                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
////                            setBody(test)
////                        }
////
////                        req.requestHandled shouldEqual true
////                        req.response.status() shouldEqual HttpStatusCode.BadRequest
////                    }
//                    it("Legg til rettighet med tomt domene skal feile med 'bad request'") {
//                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("1234", "1", "123123123", RettighetType.Les, ""))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                    it("Legg til rettighet som er gyldig, skal gi ok respons") {
//                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("1234", "1", "123123123", RettighetType.Les, "*.nav.no"))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.OK
//                    }
//                    it("Legg til rettighet som allerede eksisterer, skal feile med 'bad request'") {
//                        testEnvironment.mock.srrAddResponse = AddRightResponseList().apply {
//                            addRightResponse.add(
//                                xmlMapper.readValue(
//                                    "<AddRightResponse>\n" +
//                                        "       <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
//                                        "        <Reportee>958995369</Reportee>\n" +
//                                        "        <Right>Read</Right>\n" +
//                                        "        <ValidTo>2020-12-03T00:00:00</ValidTo>\n" +
//                                        "        <OperationResult>RULE_ALREADY_EXISTS</OperationResult>\n" +
//                                        "        </AddRightResponse>\n",
//                                    AddRightResponse::class.java
//                                )
//                            )
//                        }
//                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("1234", "1", "123123123", RettighetType.Les, "*.nav.no"))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                    it("Legg til rettighet for virksomhet som ikke finnes i ER, skal feile med 'bad request'") {
//                        testEnvironment.mock.srrAddResponse = AddRightResponseList().apply {
//                            addRightResponse.add(
//                                xmlMapper.readValue(
//                                    "<AddRightResponse>\n" +
//                                        "    <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
//                                        "    <Reportee>958995367</Reportee>\n" +
//                                        "    <Right>Read</Right>\n" +
//                                        "    <ValidTo>2020-12-03T00:00:00</ValidTo>\n" +
//                                        "    <OperationResult>EMPTY_OR_NOT_A_VALID_SSN_OR_ORGANISATION</OperationResult>\n" +
//                                        "    </AddRightResponse>\n",
//                                    AddRightResponse::class.java
//                                )
//                            )
//                        }
//                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("1234", "1", "123123123", RettighetType.Les, "*.nav.no"))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                    it("** Ekstra, hvis og når vi åpner for dato ** Legg til rettighet for virksomhet med en dato tilbake i tid, skal feile med 'bad request'") {
//                        testEnvironment.mock.srrAddResponse = AddRightResponseList().apply {
//                            addRightResponse.add(
//                                xmlMapper.readValue(
//                                    "<AddRightResponse>\n" +
//                                        "    <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
//                                        "    <Reportee>958995367</Reportee>\n" +
//                                        "    <Right>Read</Right>\n" +
//                                        "    <ValidTo>2018-12-03T00:00:00</ValidTo>\n" +
//                                        "    <OperationResult>Right_Already_Expired</OperationResult>\n" +
//                                        "    </AddRightResponse>\n",
//                                    AddRightResponse::class.java
//                                )
//                            )
//                        }
//                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("1234", "1", "123123123", RettighetType.Les, "*.nav.no"))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                    it("Legg til rettighet feiler pga ukjent årsak, skal feile med 'bad request'") {
//                        testEnvironment.mock.srrAddResponse = AddRightResponseList().apply {
//                            addRightResponse.add(
//                                xmlMapper.readValue(
//                                    "<AddRightResponse>\n" +
//                                        "    <Condition>ALLOWEDREDIRECTDOMAIN:*.TULL.ALTINN.NO;*.TEST.ALTINN.NO</Condition>\n" +
//                                        "    <Reportee>958995367</Reportee>\n" +
//                                        "    <Right>Read</Right>\n" +
//                                        "    <ValidTo>2018-12-03T00:00:00</ValidTo>\n" +
//                                        "    <OperationResult>Unknown</OperationResult>\n" +
//                                        "    </AddRightResponse>\n",
//                                    AddRightResponse::class.java
//                                )
//                            )
//                        }
//                        val test = objectMapper.writeValueAsString(PostLeggTilRettighetBody("1234", "1", "123123123", RettighetType.Les, "*.nav.no"))
//                        val req = handleRequest(HttpMethod.Post, "/api/v1/altinn/rettighetsregister/leggtil") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                            setBody(test)
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                }
//            }
//
//            afterGroup {
//                InMemoryLDAPServer.stop()
//            }
//        }
//        context("DELETE Route /api/v1/altinn/rettighetsregister/fjern") {
//            val engine = TestApplicationEngine(createTestEnvironment())
//            val testEnvironment = Environment()
//
//            beforeGroup {
//                InMemoryLDAPServer.start()
//                engine.start(wait = false)
//                engine.application.mainModule(testEnvironment, applicationState = applicationState)
//            }
//            with(engine) {
//                context("Route /api/v1/altinn/rettighetsregister/slett/{tjeneste}/{orgnr}/{lesEllerSkriv}/{domene}") {
//                    it("Slett rettighet med ugyldig tjeneste skal feile med 'bad request'") {
//                        val tjeneste = "Samtykke_UFORE"
//                        val orgnr = "123123123"
//                        val lesEllerSkriv = RettighetType.Les
//                        val domene = "*.nav.no"
//                        val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
//                        val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                    it("Slett rettighet med ugyldig virksomhetsnummer skal feile med 'bad request'") {
//                        val tjeneste = "Samtykke_AAP"
//                        val orgnr = "123"
//                        val lesEllerSkriv = RettighetType.Les
//                        val domene = "*.nav.no"
//                        val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
//                        val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                    it("Slett rettighet med ugyldig lesEllerSkriv skal feile med 'bad request'") {
//                        val tjeneste = "Samtykke_AAP"
//                        val orgnr = "123123123"
//                        val lesEllerSkriv = "dust"
//                        val domene = "*.nav.no"
//                        val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
//                        val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
//                    }
//                    it("Slett rettighet med tomt domene skal feile med 'bad request'") {
//                        val tjeneste = "Samtykke_AAP"
//                        val orgnr = "123123123"
//                        val lesEllerSkriv = RettighetType.Les
//                        val domene = " "
//                        val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
//                        val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
//                    it("Slett rettighet med alle param gyldig skal gi ok respons") {
//                        val tjeneste = "Samtykke_AAP"
//                        val orgnr = "123123123"
//                        val lesEllerSkriv = RettighetType.Les
//                        val domene = "*.nav.no"
//                        val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
//                        val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.OK
//                    }
//                    it("Slett rettighet med ukjent regel skal feile med 'bad request'") {
//                        testEnvironment.mock.srrDeleteResponse = DeleteRightResponseList().apply {
//                            deleteRightResponse.add(
//                                xmlMapper.readValue(
//                                    "<DeleteRightResponse>\n" +
//                                        "    <Condition>AllowedRedirectDomain:*.tull.altinn.no;*.test.altinn.no</Condition>\n" +
//                                        "    <Reportee>958995369</Reportee>\n" +
//                                        "    <Right>Read</Right>\n" +
//                                        "    <OperationResult>Rule_Not_Found</OperationResult>\n" +
//                                        "    </DeleteRightResponse>",
//                                    DeleteRightResponse::class.java
//                                )
//                            )
//                        }
//                        val tjeneste = "Samtykke_AAP"
//                        val orgnr = "123123123"
//                        val lesEllerSkriv = RettighetType.Les
//                        val domene = "*.nav.no"
//                        val params = "/$tjeneste/$orgnr/$lesEllerSkriv/$domene"
//                        val req = handleRequest(HttpMethod.Delete, "/api/v1/altinn/rettighetsregister/slett$params") {
//                            addHeader(HttpHeaders.Accept, "application/json")
//                            addHeader("Content-Type", "application/json")
//                            addHeader(HttpHeaders.Authorization, "Basic ${encodeBase64("n000001:itest1".toByteArray())}")
//                        }
//                        req.response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                    }
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
//                }
//            }
//
//            afterGroup {
//                InMemoryLDAPServer.stop()
//            }
//        }
//    }
//
//    // Hent rettigheter for virksomhetsnummer
//    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
//        describe("GET Hent rettigheter for et virksomhetsnummer.") {
//            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent/tjenester/org/Samtykke_AAP/123")) {
//                it("Hent rettigheter for et virksomhetsnummer 123, skal feile med 'bad request'") {
//                    response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                }
//            }
//        }
//    }
//    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
//        describe("GET Hent rettigheter for en virksomhet.") {
//            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent/tjenester/org/Samtykke_AAP/123123123")) {
//                it("Hent rettigheter for en virksomhet, skal være ok") {
//                    response.status() shouldBeEqualTo HttpStatusCode.OK
//                }
//            }
//        }
//    }
//
//    // Hent rettigheter på tjeneste eller tjenesteutgaver
//    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
//        describe("GET Hent rettigheter for alle virksomheter.") {
//            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent/tjenester/Samtykke_AAP")) {
//                it("Hent rettigheter på tjeneste for alle virksomheter, skal være ok") {
//                    response.status() shouldBeEqualTo HttpStatusCode.OK
//                }
//            }
//        }
//    }
//    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
//        describe("GET Hent rettigheter på tjenesteutgave for alle virksomheter.") {
//            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent/tjeneste/Samtykke_UFORE")) {
//                it("Hent rettigheter på tjenesteutgave for alle virksomheter, skal feile") {
//                    response.status() shouldBeEqualTo HttpStatusCode.BadRequest
//                }
//            }
//        }
//    }
//    withTestApplication(moduleFunction = { mainModule(Environment(), applicationState) }) {
//        describe("GET Hent rettigheter på tjenesteutgave for alle virksomheter.") {
//            with(handleRequest(HttpMethod.Get, "/api/v1/altinn/rettighetsregister/hent/tjeneste/Samtykke_AAP")) {
//                it("Hent rettigheter på tjenesteutgave for alle virksomheter, skal være ok") {
//                    response.status() shouldBeEqualTo HttpStatusCode.OK
//                }
//            }
//        }
//    }
//})

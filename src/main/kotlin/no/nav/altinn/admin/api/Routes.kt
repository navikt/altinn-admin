package no.nav.altinn.admin.api

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.altinn.admin.service.srr.AltinnSRRService
import no.nav.altinn.admin.service.srr.altinnsrrservice

fun Routing.api(
    altinnSrrService: AltinnSRRService
) {
    route("api") {
        route("v1") {
            authenticate {
                altinnsrrservice(altinnSrrService)
            }

            // TODO: remove
//            get("token") {
//                val accessToken = idPortenService.requestToken()
//                call.respond(accessToken)
//            }
        }
    }
}

fun Routing.nais(
    readinessCheck: () -> Boolean,
    livenessCheck: () -> Boolean = { true },
    collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
) {
    route("/internal") {
        get("/is_alive") {
            if (livenessCheck()) {
                call.respondText("Alive")
            } else {
                call.respondText("Not alive", status = HttpStatusCode.InternalServerError)
            }
        }
        get("/is_ready") {
            if (readinessCheck()) {
                call.respondText("Ready")
            } else {
                call.respondText("Not ready", status = HttpStatusCode.InternalServerError)
            }
        }
        get("/prometheus") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }
    }
}

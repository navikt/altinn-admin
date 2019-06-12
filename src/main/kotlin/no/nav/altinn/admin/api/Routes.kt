package no.nav.altinn.admin.api

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.ldap.LDAPAuthenticate

fun Routing.nais(
    environment: Environment,
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
            val ldapOk = LDAPAuthenticate(environment.application).use { ldapAuthenticate -> ldapAuthenticate.connectionOk }
            if (!ldapOk) {
                call.respondText("LDAP authenticate is not ready", status = HttpStatusCode.InternalServerError)
            }
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

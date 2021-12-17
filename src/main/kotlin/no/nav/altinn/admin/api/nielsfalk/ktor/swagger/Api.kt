package no.nav.altinn.admin.api.nielsfalk.ktor.swagger

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Found
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.delete
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.locations.put
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.util.pipeline.PipelineContext
import java.util.Locale
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.altinn.admin.AUTHENTICATION_BEARER
import no.nav.altinn.admin.swagger

/**
 * @author Niels Falk, changed by Torstein Nesby
 */

sealed class Security
data class NoSecurity(val secSetting: List<Map<String, List<String>>> = emptyList()) : Security()
data class BearerTokenSecurity(
    val secSetting: List<Map<String, List<String>>> = listOf(mapOf("bearerAuth" to emptyList()))
) : Security()

data class Metadata(
    val responses: Map<HttpStatusCode, KClass<*>>,
    val summary: String = "",
    val security: Security = NoSecurity()
) {

    var headers: KClass<*>? = null
    var parameter: KClass<*>? = null

    inline fun <reified T> header(): Metadata {
        this.headers = T::class
        return this
    }

    inline fun <reified T> parameter(): Metadata {
        this.parameter = T::class
        return this
    }
}

@KtorExperimentalLocationsAPI
inline fun <reified LOCATION : Any, reified ENTITY_TYPE : Any> Metadata.apply(method: HttpMethod) {
    val clazz = LOCATION::class.java
    val location = clazz.getAnnotation(Location::class.java)
    val tags = clazz.getAnnotation(Group::class.java)
    applyResponseDefinitions()
    applyOperations(location, tags, method, LOCATION::class, ENTITY_TYPE::class)
}

fun Metadata.applyResponseDefinitions() =
    responses.values.forEach { addDefinition(it) }

@KtorExperimentalLocationsAPI
fun <LOCATION : Any, BODY_TYPE : Any> Metadata.applyOperations(
    location: Location,
    group: Group?,
    method: HttpMethod,
    locationType: KClass<LOCATION>,
    entityType: KClass<BODY_TYPE>
) {
    swagger.paths
        .getOrPut(location.path) { mutableMapOf() }[method.value.lowercase(Locale.getDefault())] =
        Operation(this, location, group, locationType, entityType, method)
}

fun String.responds(vararg pairs: Pair<HttpStatusCode, KClass<*>>): Metadata =
    Metadata(responses = mapOf(*pairs), summary = this)

fun String.securityAndResponse(security: Security, vararg pairs: Pair<HttpStatusCode, KClass<*>>): Metadata =
    Metadata(responses = mapOf(*pairs), summary = this, security = security)

inline fun <reified T> ok(): Pair<HttpStatusCode, KClass<*>> = OK to T::class
inline fun <reified T> noContent(): Pair<HttpStatusCode, KClass<*>> = NoContent to T::class
inline fun <reified T> found(): Pair<HttpStatusCode, KClass<*>> = Found to T::class
inline fun <reified T> failed(): Pair<HttpStatusCode, KClass<*>> = InternalServerError to T::class
inline fun <reified T> serviceUnavailable(): Pair<HttpStatusCode, KClass<*>> = ServiceUnavailable to T::class
inline fun <reified T> badRequest(): Pair<HttpStatusCode, KClass<*>> = BadRequest to T::class
inline fun <reified T> unAuthorized(): Pair<HttpStatusCode, KClass<*>> = Unauthorized to T::class

@KtorExperimentalLocationsAPI
inline fun <reified LOCATION : Any, reified ENTITY : Any> Route.post(
    metadata: Metadata,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, ENTITY) -> Unit
): Route {

    log.info("Generating swagger spec for POST ${LOCATION::class.java.getAnnotation(Location::class.java)}")
    metadata.apply<LOCATION, ENTITY>(HttpMethod.Post)

    return when (metadata.security) {
        is NoSecurity -> post<LOCATION> { body(this, it, withContext(Dispatchers.IO) { call.receive() }) }
        is BearerTokenSecurity -> authenticate(AUTHENTICATION_BEARER) { post<LOCATION> { body(this, it, withContext(Dispatchers.IO) { call.receive() }) } }
    }
}

@KtorExperimentalLocationsAPI
inline fun <reified LOCATION : Any, reified ENTITY : Any> Route.put(
    metadata: Metadata,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION, ENTITY) -> Unit
): Route {

    log.info("Generating swagger spec for PUT ${LOCATION::class.java.getAnnotation(Location::class.java)}")
    metadata.apply<LOCATION, ENTITY>(HttpMethod.Put)

    return when (metadata.security) {
        is NoSecurity -> put<LOCATION> { body(this, it, call.receive()) }
        is BearerTokenSecurity -> authenticate(AUTHENTICATION_BEARER) { put<LOCATION> { body(this, it, call.receive()) } }
    }
}

@KtorExperimentalLocationsAPI
inline fun <reified LOCATION : Any> Route.get(
    metadata: Metadata,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {

    log.info("Generating swagger spec for GET ${LOCATION::class.java.getAnnotation(Location::class.java)}")
    metadata.apply<LOCATION, Unit>(HttpMethod.Get)

    return when (metadata.security) {
        is NoSecurity -> get<LOCATION> { body(this, it) }
        is BearerTokenSecurity -> authenticate(AUTHENTICATION_BEARER) { get<LOCATION> { body(this, it) } }
    }
}

@KtorExperimentalLocationsAPI
inline fun <reified LOCATION : Any> Route.delete(
    metadata: Metadata,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(LOCATION) -> Unit
): Route {

    log.info("Generating swagger spec for DELETE ${LOCATION::class.java.getAnnotation(Location::class.java)}")
    metadata.apply<LOCATION, Unit>(HttpMethod.Delete)

    return when (metadata.security) {
        is NoSecurity -> delete<LOCATION> { body(this, it) }
        is BearerTokenSecurity -> authenticate(AUTHENTICATION_BEARER) { delete<LOCATION> { body(this, it) } }
    }
}

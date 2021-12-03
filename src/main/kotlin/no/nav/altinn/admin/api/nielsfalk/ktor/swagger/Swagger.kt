@file:Suppress("MemberVisibilityCanPrivate", "unused")

package no.nav.altinn.admin.api.nielsfalk.ktor.swagger

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import mu.KotlinLogging
import no.nav.altinn.admin.swagger

/**
 * @author Niels Falk, changed by Torstein Nesby
 */
val log = KotlinLogging.logger { }

typealias ModelName = String

typealias PropertyName = String
typealias Path = String
typealias Definitions = MutableMap<ModelName, ModelData>
typealias Paths = MutableMap<Path, Methods>
typealias MethodName = String
typealias HttpStatus = String
typealias Methods = MutableMap<MethodName, Operation>
typealias Content = MutableMap<String, MutableMap<String, ModelReference?>>

data class Key(
    val description: String,
    val type: String,
    val name: String,
    val `in`: String
)

data class Swagger(
    val openapi: String = "3.0.0",
    val swagger: String = "3.0",
    val info: Information,
    val paths: Paths = mutableMapOf(),
    val components: Components = Components(
        SecuritySchemes(
            BearerAuth(
                type = "http",
                scheme = "bearer",
                bearerFormat = "JWT"
            )
        )
    )
)

data class Components(
    val securitySchemes: SecuritySchemes,
    val schemas: Definitions = mutableMapOf()
)

data class SecuritySchemes(
    val bearerAuth: BearerAuth
)

data class BearerAuth(
    val type: String,
    val scheme: String,
    val bearerFormat: String
)

data class Information(
    val description: String,
    val version: String,
    val title: String,
    val contact: Contact
)

data class Contact(
    val name: String,
    val url: String,
    val email: String
)

data class Tag(
    val name: String
)

@KtorExperimentalLocationsAPI
class Operation(
    metadata: Metadata,
    location: Location,
    group: Group?,
    locationType: KClass<*>,
    entityType: KClass<*>,
    method: HttpMethod
) {
    val tags = group?.toList()
    val summary = metadata.summary
    val parameters = setParameterList(entityType, locationType, location, metadata, method)
    val requestBody = setRequestBody(entityType, locationType, location, metadata, method)

    val responses: Map<HttpStatus, Response> = metadata.responses.map {
        val (status, kClass) = it
        addDefinition(kClass)
        status.value.toString() to Response(status, kClass)
    }.toMap()

    val security = when (metadata.security) {
        is NoSecurity -> metadata.security.secSetting
        is BearerTokenSecurity -> metadata.security.secSetting
    }
}

@KtorExperimentalLocationsAPI
private fun setRequestBody(
    entityType: KClass<*>,
    locationType: KClass<*>,
    location: Location,
    metadata: Metadata,
    method: HttpMethod
): Any? {
    if (method.value == "POST" || method.value == "PUT") {
        return mutableListOf<RequestBody>().apply {
            if (entityType != Unit::class) {
                addDefinition(entityType)
                add(entityType.bodyRequest())
            }
            addAll(locationType.memberProperties.map { it.toRequestBody(location.path) })
            metadata.parameter?.let {
                addAll(it.memberProperties.map { it.toRequestBody(location.path, ParameterInputType.query) })
            }
            metadata.headers?.let {
                addAll(it.memberProperties.map { it.toRequestBody(location.path, ParameterInputType.header) })
            }
        }.firstOrNull() ?: emptyList<RequestBody>()
    } else {
        return null
    }
}

@KtorExperimentalLocationsAPI
private fun setParameterList(
    entityType: KClass<*>,
    locationType: KClass<*>,
    location: Location,
    metadata: Metadata,
    method: HttpMethod
): List<Parameter> {
    if (method.value == "GET" || method.value == "DELETE" || method.value == "HEAD") {
        return mutableListOf<Parameter>().apply {
            if (entityType != Unit::class) {
                addDefinition(entityType)
                add(entityType.bodyParameter())
            }
            addAll(locationType.memberProperties.map { it.toParameter(location.path) })
            metadata.parameter?.let {
                addAll(it.memberProperties.map { it.toParameter(location.path, ParameterInputType.query) })
            }
            metadata.headers?.let {
                addAll(it.memberProperties.map { it.toParameter(location.path, ParameterInputType.header) })
            }
        }
    } else {
        return emptyList()
    }
}

private fun Group.toList(): List<Tag> {
    return listOf(Tag(name))
}

fun <T, R> KProperty1<T, R>.toParameter(
    path: String,
    inputType: ParameterInputType =
        if (path.contains("{$name}"))
            ParameterInputType.path
        else
            ParameterInputType.query
): Parameter {
    return Parameter(
        toModelProperty(),
        name,
        inputType,
        required = !returnType.isMarkedNullable
    )
}

private fun KClass<*>.bodyParameter() =
    Parameter(
        referenceProperty(),
        name = "body",
        description = modelName(),
        `in` = ParameterInputType.body
    )

@KtorExperimentalLocationsAPI
fun <T, R> KProperty1<T, R>.toRequestBody(
    path: String,
    inputType: ParameterInputType =
        if (path.contains("{$name}"))
            ParameterInputType.path
        else
            ParameterInputType.query
): RequestBody {
    return RequestBody(
        toModelProperty(),
        required = !returnType.isMarkedNullable
    )
}

private fun KClass<*>.bodyRequest() =
    RequestBody(
        referenceProperty(),
        description = modelName()
    )

class Response(httpStatusCode: HttpStatusCode, kClass: KClass<*>) {
    val description = if (kClass == Unit::class) httpStatusCode.description else kClass.responseDescription()
    val schema = if (kClass == Unit::class) null else ModelReference("#/components/schemas/" + kClass.modelName())
}

fun KClass<*>.responseDescription(): String = modelName()

class ModelReference(val `$ref`: String)

class RequestBody(
    property: Property,
    val description: String = property.description,
    val required: Boolean = true,
    val content: MutableMap<String, MutableMap<String, ModelReference>> = mutableMapOf(
        "application/json" to mutableMapOf(
            "schema" to ModelReference(property.`$ref`)
        )
    )
)

class Parameter(
    property: Property,
    val name: String,
    val `in`: ParameterInputType,
    val description: String = property.description,
    val required: Boolean = true,
    val type: String? = property.type,
    val format: String? = property.format,
    val enum: List<String>? = property.enum,
    val items: Property? = property.items,
    val schema: ModelReference? = ModelReference(property.`$ref`)
)

enum class ParameterInputType {
    query, path, body, header
}

class ModelData(kClass: KClass<*>) {
    val properties: Map<PropertyName, Property> =
        kClass.memberProperties
            .map { it.name to it.toModelProperty() }
            .toMap()
}

private const val DATE_TIME: String = "date-time"

val propertyTypes = mapOf(
    Int::class to Property("integer", "int32"),
    Long::class to Property("integer", "int64"),
    String::class to Property("string"),
    Double::class to Property("number", "double"),
    Instant::class to Property("string", DATE_TIME),
    Date::class to Property("string", DATE_TIME),
    LocalDateTime::class to Property("string", DATE_TIME),
    LocalDate::class to Property("string", "date")
).mapKeys { it.key.qualifiedName }

fun <T, R> KProperty1<T, R>.toModelProperty(): Property =
    (returnType.classifier as KClass<*>)
        .toModelProperty(returnType)

private fun KClass<*>.toModelProperty(returnType: KType? = null): Property =
    propertyTypes[qualifiedName?.removeSuffix("?")]
        ?: if (returnType != null && (isSubclassOf(Collection::class) || this.isSubclassOf(Set::class))) {
            val kClass: KClass<*> = returnType.arguments.first().type?.classifier as KClass<*>
            Property(items = kClass.toModelProperty(), type = "array")
        } else if (returnType != null && this.isSubclassOf(Map::class)) {
            Property(type = "object")
        } else if (returnType != null && this.isSubclassOf(String::class)) {
            Property(type = "string")
        } else if (java.isEnum) {
            val enumConstants = (this).java.enumConstants
            Property(enum = enumConstants.map { (it as Enum<*>).name }, type = "string")
        } else {
            addDefinition(this)
            referenceProperty()
        }

private fun KClass<*>.referenceProperty(): Property =
    Property(
        `$ref` = "#/components/schemas/" + modelName(),
        description = modelName(),
        type = null
    )

open class Property(
    val type: String?,
    val format: String = "",
    val enum: List<String>? = null,
    val items: Property? = null,
    val description: String = "",
    val `$ref`: String = ""
)

fun addDefinition(kClass: KClass<*>) {
    if ((kClass != Unit::class) && !swagger.components.schemas.containsKey(kClass.modelName())) {
        log.info("Generating swagger spec for model ${kClass.modelName()}")
        swagger.components.schemas[kClass.modelName()] = ModelData(kClass)
    }
}

private fun KClass<*>.modelName(): ModelName = simpleName ?: toString()

annotation class Group(val name: String)

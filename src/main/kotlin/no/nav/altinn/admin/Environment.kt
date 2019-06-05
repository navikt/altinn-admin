package no.nav.altinn.admin

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.altinn.schemas.services.register.srr._2015._06.*
import no.nav.altinn.admin.common.xmlMapper
import java.io.File

//private const val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/application.properties"

private val config = if (System.getenv("APPLICATION_PROFILE") == "remote") {
    systemProperties() overriding
        EnvironmentVariables() overriding
        //ConfigurationProperties.fromFile(File(vaultApplicationPropertiesPath)) overriding
        ConfigurationProperties.fromResource("application.properties")
} else {
    systemProperties() overriding
        EnvironmentVariables() overriding
        ConfigurationProperties.fromResource("application.properties")
}

data class Environment(
    val stsUrl: String = config[Key("sts.url", stringType)],

    val altinn: Altinn = Altinn(),
    val application: Application = Application(),
    val jwt: Jwt = Jwt(),
    val mock: Mock = Mock()

) {

    data class Altinn(
        val altinnAdminUrl: String = config[Key("altinn.admin.url", stringType)],
        val username: String = config[Key("altinn.username", stringType)],
        val password: String = config[Key("altinn.password", stringType)]
    )

    data class Mock (
            private val srrAddXmlResponse : String? = config[Key("mock.ssr.add.response", stringType)],
            private val srrDeleteXmlResponse : String? = config[Key("mock.ssr.delete.response", stringType)],
            private val srrGetXmlResponse : String? = config[Key("mock.ssr.get.response", stringType)],
            val srrAddResponse: AddRightResponseList? = if (srrAddXmlResponse.isNullOrEmpty()) { null }
                                                        else { AddRightResponseList().apply { addRightResponse.add(
                                                            xmlMapper.readValue(srrAddXmlResponse, AddRightResponse::class.java))}},
            val srrDeleteResponse: DeleteRightResponseList? = if (srrDeleteXmlResponse.isNullOrEmpty()) { null }
            else { DeleteRightResponseList().apply { deleteRightResponse.add(
                    xmlMapper.readValue(srrDeleteXmlResponse, DeleteRightResponse::class.java))}},
            val srrGetResponse : GetRightResponseList? = if (srrGetXmlResponse.isNullOrEmpty()) { null }
            else { GetRightResponseList().apply {  getRightResponse.add(
                    xmlMapper.readValue(srrGetXmlResponse, GetRightResponse::class.java))}}

            //AddRightResponseList().apply { addRightResponse.add(
            //        xmlMapper.readValue(config[Key("mock.ssr.add.response", stringType)], AddRightResponse::class.java))}
    )

    data class Application(
        val devProfile: Boolean = config[Key("application.profile", stringType)] == "local",
        val port: Int = config[Key("application.port", intType)],
        val username: String = config[Key("serviceuser.username", stringType)],
        val password: String = config[Key("serviceuser.password", stringType)]
    )


    data class Jwt(
        val audience: String = config[Key("jwt.audience", stringType)],
        val issuer: String = config[Key("jwt.issuer", stringType)],
        val jwksUri: String = config[Key("jwt.jwks.uri", stringType)]
    )

}


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
import no.nav.altinn.admin.ldap.LDAPBase
import java.io.File

private const val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/application.properties"

private val config = if (System.getenv("APPLICATION_PROFILE") != "local") {
    systemProperties() overriding
        EnvironmentVariables() overriding
        ConfigurationProperties.fromFile(File(vaultApplicationPropertiesPath)) overriding
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
    val mock: Mock = Mock()
) {

    data class Altinn(
        val altinnAdminUrl: String = config[Key("altinn.admin.url", stringType)],
        val username: String = config[Key("altinn.username", stringType)],
        val password: String = config[Key("altinn.password", stringType)]
    )

    data class Application(
        val devProfile: Boolean = config[Key("application.profile", stringType)] == "local",
        val port: Int = config[Key("application.port", intType)],
        val username: String = config[Key("serviceuser.username", stringType)],
        val password: String = config[Key("serviceuser.password", stringType)],
        val users: String = config[Key("approved.users.list", stringType)],

        // common ldap details for both authentication and group management
        val ldapConnTimeout: Int = config[Key("ldap.conntimeout", intType)],
        val ldapUserAttrName: String = config[Key("ldap.userattrname", stringType)],

        // ldap authentication details - production LDAP
        val ldapAuthHost: String = config[Key("ldap.auth.host", stringType)],
        val ldapAuthPort: Int = config[Key("ldap.auth.port", intType)],
        val ldapAuthUserBase: String = config[Key("ldap.auth.userbase", stringType)],

        // ldap details for managing ldap groups - different LDAP servers (test, preprod, production)
        val ldapHost: String = config[Key("ldap.host", stringType)],
        val ldapPort: Int = config[Key("ldap.port", intType)],

        val ldapSrvUserBase: String = config[Key("ldap.srvuserbase", stringType)],
        val ldapGroupBase: String = config[Key("ldap.groupbase", stringType)],
        val ldapGroupAttrName: String = config[Key("ldap.groupattrname", stringType)],
        val ldapGrpMemberAttrName: String = config[Key("ldap.grpmemberattrname", stringType)]
    )

    data class Mock(
        private val srrAddXmlResponse: String? = config[Key("mock.ssr.add.response", stringType)],
        private val srrDeleteXmlResponse: String? = config[Key("mock.ssr.delete.response", stringType)],
        private val srrGetXmlResponse: String? = config[Key("mock.ssr.get.response", stringType)],
        val srrAddResponse: AddRightResponseList? = if (srrAddXmlResponse.isNullOrEmpty()) { null } else { AddRightResponseList().apply { addRightResponse.add(
                xmlMapper.readValue(srrAddXmlResponse, AddRightResponse::class.java)) } },
        val srrDeleteResponse: DeleteRightResponseList? = if (srrDeleteXmlResponse.isNullOrEmpty()) { null } else { DeleteRightResponseList().apply { deleteRightResponse.add(
                xmlMapper.readValue(srrDeleteXmlResponse, DeleteRightResponse::class.java)) } },
        val srrGetResponse: GetRightResponseList? = if (srrGetXmlResponse.isNullOrEmpty()) { null } else { GetRightResponseList().apply { getRightResponse.add(
                xmlMapper.readValue(srrGetXmlResponse, GetRightResponse::class.java)) } }
        // AddRightResponseList().apply { addRightResponse.add(
        //        xmlMapper.readValue(config[Key("mock.ssr.add.response", stringType)], AddRightResponse::class.java))}
    )
}

// Connection factory for which ldap in matter

enum class LdapConnectionType { AUTHENTICATION, GROUP }

fun Environment.Application.getConnectionInfo(connType: LdapConnectionType) =
        when (connType) {
            LdapConnectionType.AUTHENTICATION -> LDAPBase.Companion.ConnectionInfo(
                    ldapAuthHost, ldapAuthPort, ldapConnTimeout
            )
            LdapConnectionType.GROUP -> LDAPBase.Companion.ConnectionInfo(
                    ldapHost, ldapPort, ldapConnTimeout
            )
        }

// Return diverse distinguished name types

fun Environment.Application.userDN(user: String) = "$ldapUserAttrName=$user,$ldapAuthUserBase"

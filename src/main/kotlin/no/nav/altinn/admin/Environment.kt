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
        val devTest: String = config[Key("application.profile", stringType)],
        val devProfile: Boolean = devTest == "local",
//        val devProfile: Boolean = true,
        val port: Int = config[Key("application.port", intType)],
        val username: String = config[Key("serviceuser.username", stringType)],
        val password: String = config[Key("serviceuser.password", stringType)],

        // common ldap details for both authentication and group management
        val ldapConnTimeout: Int = System.getenv("LDAP_CONNTIMEOUT")?.toInt() ?: 2_000,
        val ldapUserAttrName: String = System.getenv("LDAP_USERATTRNAME")?.toString() ?: "",

        // ldap authentication details - production LDAP
        val ldapAuthHost: String = config[Key("LDAP_AUTH_HOST", stringType)],
        val ldapAuthPort: Int = config[Key("LDAP_AUTH_PORT", intType)],
        val ldapAuthUserBase: String = System.getenv("LDAP_AUTH_USERBASE")?.toString() ?: "",

        // ldap details for managing ldap groups - different LDAP servers (test, preprod, production)
        val ldapHost: String = config[Key("LDAP_HOST", stringType)],
        val ldapPort: Int = config[Key("LDAP_PORT", intType)],

        val ldapSrvUserBase: String = System.getenv("LDAP_SRVUSERBASE")?.toString() ?: "",
        val ldapGroupBase: String = System.getenv("LDAP_GROUPBASE")?.toString() ?: "",
        val ldapGroupAttrName: String = System.getenv("LDAP_GROUPATTRNAME")?.toString() ?: "",
        val ldapGrpMemberAttrName: String = System.getenv("LDAP_GRPMEMBERATTRNAME")?.toString() ?: "",

        // ldap user and pwd with enough authorization for managing ldap groups
        val ldapUser: String = System.getenv("LDAP_USER")?.toString() ?: "",
        val ldapPassword: String = System.getenv("LDAP_PASSWORD")?.toString() ?: ""
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

fun Environment.Application.ldapAuthenticationInfoComplete(): Boolean =
        ldapUserAttrName.isNotEmpty() && ldapAuthHost.isNotEmpty() && ldapAuthPort != 0 && ldapAuthUserBase.isNotEmpty()

fun Environment.Application.ldapGroupInfoComplete(): Boolean =
        ldapHost.isNotEmpty() && ldapPort != 0 && ldapSrvUserBase.isNotEmpty() && ldapGroupBase.isNotEmpty() &&
                ldapGroupAttrName.isNotEmpty() && ldapGrpMemberAttrName.isNotEmpty() && ldapUser.isNotEmpty() &&
                ldapPassword.isNotEmpty()

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

fun Environment.Application.srvUserDN() = "$ldapUserAttrName=$ldapUser,$ldapSrvUserBase"

fun Environment.Application.groupDN(groupName: String) = "$ldapGroupAttrName=$groupName,$ldapGroupBase"

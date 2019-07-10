package no.nav.altinn.admin.ldap

import com.unboundid.ldap.sdk.DisconnectType
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionOptions
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.util.ssl.SSLUtil
import com.unboundid.util.ssl.TrustAllTrustManager
import mu.KotlinLogging

/**
 * LDAPBase provides minimum set of services for LDAP operations
 * - establish connection to LDAPS endpoint with proper resource management by AutoCloseable
 * - check if connected
 */

abstract class LDAPBase(private val connInfo: ConnectionInfo) : AutoCloseable {

    val regexNAVIdent = """^[a-zA-Z]\d{6}$""".toRegex()

    private val connectOptions = LDAPConnectionOptions().apply {
        connectTimeoutMillis = connInfo.timeout
    }

    // NB! - TrustAllTrustManager is too trusty, but good enough when inside corporate inner zone
    protected val ldapConnection = LDAPConnection(
            SSLUtil(TrustAllTrustManager()).createSSLSocketFactory(),
            connectOptions)

    init {
        // initialize LDAP connection
        try {
            ldapConnection.connect(connInfo.host, connInfo.port)
            logger.debug { "Successfully connected to $connInfo" }
        } catch (e: LDAPException) {
            logger.error { "Exception occurred LDAP operations against $connInfo will fail - $e" }
            ldapConnection.setDisconnectInfo(
                    DisconnectType.IO_ERROR,
                    "Exception occurred when connecting to LDAPS $connInfo", e)
        }
    }

    val connectionOk = ldapConnection.isConnected

    override fun close() {
        logger.debug { "Closing ldap connection $connInfo" }
        ldapConnection.close()
    }

    protected fun isNAVIdent(userName: String): Boolean = regexNAVIdent.matches(userName)

    companion object {

        data class ConnectionInfo(val host: String, val port: Int, val timeout: Int = 2_000)

        val logger = KotlinLogging.logger { }
    }
}

package no.nav.altinn.admin.ldap

import com.unboundid.ldap.sdk.DN
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.ResultCode
import mu.KotlinLogging
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.LdapConnectionType
import no.nav.altinn.admin.getConnectionInfo
import no.nav.altinn.admin.userDN

/**
 * LDAPAuthenticate provides only canUserAuthenticate by simple LDAP bind verification
 *
 * See https://docs.ldap.com/ldap-sdk/docs/javadoc/overview-summary.html
 */

class LDAPAuthenticate(private val config: Environment.Application) :
        LDAPBase(config.getConnectionInfo(LdapConnectionType.AUTHENTICATION)) {

    fun canUserAuthenticate(user: String, pwd: String): Boolean =
            if (!ldapConnection.isConnected) {
                logger.error { "Cannot authenticate, connection to ldap is down" }
                false
            } else {
                // fold over resolved DNs, NAV ident or service accounts (normal + Basta)
                resolveDNs(user).fold(false) { acc, dn -> acc || authenticated(dn, pwd, acc) }.also {

                    val connInfo = config.getConnectionInfo(LdapConnectionType.AUTHENTICATION)
                    when (it) {
                        true -> {
                            logger.info { "Successful bind of $user to $connInfo" }
                        }
                        false -> logger.error { "Cannot bind $user to $connInfo" }
                    }
                }
            }

    fun getUsersGroupNames(user: String) {
        var s1 = ldapConnection.getEntry("dc=alf")
        logger.info { "${ldapConnection.connectionPoolName} Result is ${s1.toLDIFString()} " }
        val s2 = ldapConnection.getEntry("OU=NAV")
        logger.info { "${ldapConnection.connectionPoolName} Result is ${s2.toLDIFString()} " }
    }

    // resolve DNs for both service accounts, including those created in Basta. The order of DNs according to user name
    private fun resolveDNs(user: String): List<String> = config.userDN(user).let {

        val rdns = DN(it).rdNs
        val dnPrefix = rdns[rdns.indices.first]
        val dnPostfix = "${rdns[rdns.indices.last - 1]},${rdns[rdns.indices.last]}"
        val srvAccounts = listOf("OU=ApplAccounts,OU=ServiceAccounts", "OU=ServiceAccounts")
        logger.info { "DNs : $it" }
        if (isNAVIdent(user)) listOf(it)
        else srvAccounts.map { "$dnPrefix,$it,$dnPostfix" }
    }

    private fun authenticated(dn: String, pwd: String, alreadyAuthenticated: Boolean): Boolean =
            if (alreadyAuthenticated) true
            else
                try {
                    val bind = ldapConnection.bind(dn, pwd)
                    if (bind.resultCode == ResultCode.SUCCESS) {
                        try {
                            getUsersGroupNames(dn)
                        } catch (e: Exception) {
                            logger.info { "Exception: $e" }
                        }
                        true
                    } else {
                        false
                    }
                } catch (e: LDAPException) { false }

    companion object {

        val logger = KotlinLogging.logger { }
    }
}

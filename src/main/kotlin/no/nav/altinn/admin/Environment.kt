package no.nav.altinn.admin

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.io.File
import no.altinn.schemas.services.register.srr._2015._06.AddRightResponse
import no.altinn.schemas.services.register.srr._2015._06.AddRightResponseList
import no.altinn.schemas.services.register.srr._2015._06.DeleteRightResponse
import no.altinn.schemas.services.register.srr._2015._06.DeleteRightResponseList
import no.altinn.schemas.services.register.srr._2015._06.GetRightResponse
import no.altinn.schemas.services.register.srr._2015._06.GetRightResponseList
import no.nav.altinn.admin.common.xmlMapper

private const val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/application.properties"

private val config = if (System.getenv("APPLICATION_PROFILE") == "remote" && System.getenv("USE_VAULT_ENV") == "true") {
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
    val altinn: Altinn = Altinn(),
    val azure: Azure = Azure(),
    val maskinporten: Maskinporten = Maskinporten(),
    val application: Application = Application(),
    val srrService: SrrService = SrrService(),
    val dqService: DqService = DqService(),
    val correspondenceService: CorrespondenceService = CorrespondenceService(),
    val prefillService: PrefillService = PrefillService(),

    val mock: Mock = Mock()
) {
    data class Altinn(
        val altinnSrrUrl: String = config[Key("altinn.rettighetsregister.url", stringType)],
        val altinnDqUrl: String = config[Key("altinn.downloadqueue.url", stringType)],
        val altinnCorrespondenceUrl: String = config[Key("altinn.correspondence.url", stringType)],
        val altinnPrefillUrl: String = config[Key("altinn.prefill.url", stringType)],
        val altinnReceiptUrl: String = config[Key("altinn.receipt.url", stringType)],
        val altinnNotificationUrl: String = config[Key("altinn.notification.url", stringType)],
        val altinnSubscriptionUrl: String = config[Key("altinn.subscription.url", stringType)],
        val altinnArchiveUrl: String = config[Key("altinn.archive.url", stringType)],
        val username: String = config[Key("altinn.username", stringType)],
        val password: String = config[Key("altinn.password", stringType)]
    )

    data class Azure(
        val azureAppClientId: String = config.getOrElse(Key("azure.app.client.id", stringType), "dummyForUnitTest"),
        val azureAppClientSecret: String = config.getOrElse(Key("azure.app.client.secret", stringType), "dummyForUnitTest"),
        val azureAppWellKnownUrl: String = config.getOrElse(Key("azure.app.well.known.url", stringType), "dummyForUnitTest"),
        val azureOpenidConfigTokenEndpoint: String = config.getOrElse(Key("azure.openid.config.token.endpoint", stringType), "dummyForUnitTest"),
        var idToken: String = ""
    )

    data class Maskinporten(
        val baseUrl: String = config.getOrElse(Key("maskinporten.url", stringType), "tull.no"),
        val wellKnownUrl: String = config.getOrElse(Key("maskinporten.well.known.url", stringType), "tull.no"),
        val clientId: String = config.getOrElse(Key("maskinporten.client.id", stringType), "en-id-key-her"),
        val clientJwk: String = config.getOrElse(Key("maskinporten.client.jwk", stringType), "tull-jwk"),
        val scopes: String = config.getOrElse(Key("maskinporten.scopes", stringType), "altinn:scope"),
        val issuer: String = config.getOrElse(Key("maskinporten.issuer", stringType), "maskinporten"),
        var token: String = config.getOrElse(Key("maskinporten.token", stringType), "maskinporten")
    )

    data class Application(
        val devProfile: Boolean = config[Key("application.profile", stringType)] == "local",
        val localEnv: String = config[Key("application.env", stringType)],
        val port: Int = config[Key("application.port", intType)],
        val users: String = config[Key("approved.users.list", stringType)],
        val baseUrl: String = config[Key("application.baseurl", stringType)],
    )

    data class SrrService(
        val serviceCodes: String = config[Key("srr.servicecode.list", stringType)],
        val srrExpireDate: String = config[Key("srr.expiring.date", stringType)]
    )

    data class DqService(
        val serviceCodes: String = config[Key("dq.servicecode.list", stringType)]
    )

    data class CorrespondenceService(
        val serviceCodes: String = config[Key("correspondence.servicecode.list", stringType)]
    )

    data class PrefillService(
        val serviceCodes: String = config[Key("prefill.servicecode.list", stringType)]
    )

    data class Mock(
        private val isCloud: Boolean = config[Key("application.profile", stringType)] != "local",
        private val srrAddXmlResponse: String? = config[Key("mock.ssr.add.response", stringType)],
        private val srrDeleteXmlResponse: String? = config[Key("mock.ssr.delete.response", stringType)],
        private val srrGetXmlResponse: String? = config[Key("mock.ssr.get.response", stringType)],
        var srrAddResponse: AddRightResponseList? = if (isCloud) {
            null
        } else {
            AddRightResponseList().apply {
                addRightResponse.add(
                    xmlMapper.readValue(srrAddXmlResponse, AddRightResponse::class.java)
                )
            }
        },
        var srrDeleteResponse: DeleteRightResponseList? = if (isCloud) {
            null
        } else {
            DeleteRightResponseList().apply {
                deleteRightResponse.add(
                    xmlMapper.readValue(srrDeleteXmlResponse, DeleteRightResponse::class.java)
                )
            }
        },
        var srrGetResponse: GetRightResponseList? = if (isCloud) {
            null
        } else {
            GetRightResponseList().apply {
                getRightResponse.add(
                    xmlMapper.readValue(srrGetXmlResponse, GetRightResponse::class.java)
                )
            }
        }
        // AddRightResponseList().apply { addRightResponse.add(
        //        xmlMapper.readValue(config[Key("mock.ssr.add.response", stringType)], AddRightResponse::class.java))}
    )
}

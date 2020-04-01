package no.nav.altinn.admin.ws

import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.altinn.services.intermediary.receipt._2009._10.IReceiptAgencyExternalBasic
import no.altinn.services.register.srr._2015._06.IRegisterSRRAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

object Clients {
    fun iRegisterSRRAgencyExternalBasic(serviceUrl: String): IRegisterSRRAgencyExternalBasic =
        createServicePort(
            serviceUrl = serviceUrl,
            serviceClazz = IRegisterSRRAgencyExternalBasic::class.java
        )

    fun iDownloadQueueExternalBasic(serviceUrl: String): IDownloadQueueExternalBasic =
        createServicePort(
            serviceUrl = serviceUrl,
            serviceClazz = IDownloadQueueExternalBasic::class.java
        )

    fun iCorrespondenceExternalBasic(serviceUrl: String): ICorrespondenceAgencyExternalBasic =
        createServicePort(
            serviceUrl = serviceUrl,
            serviceClazz = ICorrespondenceAgencyExternalBasic::class.java
        )

    fun iReceiptAgencyExternalBasic(serviceUrl: String): IReceiptAgencyExternalBasic =
        createServicePort(
            serviceUrl = serviceUrl,
            serviceClazz = IReceiptAgencyExternalBasic::class.java
        )

    private fun <PORT_TYPE> createServicePort(
        serviceUrl: String,
        serviceClazz: Class<PORT_TYPE>
    ): PORT_TYPE = JaxWsProxyFactoryBean().apply {
        address = serviceUrl
        serviceClass = serviceClazz
        features = listOf(WSAddressingFeature(), LoggingFeature())
    }.create(serviceClazz)
}

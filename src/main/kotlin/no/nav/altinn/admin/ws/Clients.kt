package no.nav.altinn.admin.ws

import no.altinn.services.register.srr._2015._06.IRegisterSRRAgencyExternalBasic
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature

object Clients {
    fun iRegisterSRRAgencyExternalBasic(serviceUrl: String): IRegisterSRRAgencyExternalBasic =
        createServicePort(
            serviceUrl = serviceUrl,
            serviceClazz = IRegisterSRRAgencyExternalBasic::class.java
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

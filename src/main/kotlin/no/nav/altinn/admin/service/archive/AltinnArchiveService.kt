package no.nav.altinn.admin.service.archive

import mu.KotlinLogging
import no.altinn.schemas.services.archive.shipmentstatus._2014._02.SearchArchiveShipmentStatusBE
import no.altinn.services.archive.serviceownerarchive._2009._10.IServiceOwnerArchiveExternalBasic
import no.altinn.services.archive.serviceownerarchive._2009._10.IServiceOwnerArchiveExternalBasicTestAltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.dateToXmlGregorianCalendar
import no.nav.altinn.admin.common.isDate

private val logger = KotlinLogging.logger { }
class AltinnArchiveService(env: Environment, iArchiveAgencyExternalBasicFactory: () -> IServiceOwnerArchiveExternalBasic) {

    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iArchiveAgencyExternalBasic by lazy(iArchiveAgencyExternalBasicFactory)

    fun getArchiveStatus(dateFrom: String, dateTo: String) {
        try {
            if (!isDate(dateFrom) || !isDate(dateTo))
                throw IllegalArgumentException("Wrong date format")

            val d1 = dateToXmlGregorianCalendar(dateFrom)
            val d2 = dateToXmlGregorianCalendar(dateTo)
            val statusItems = iArchiveAgencyExternalBasic.getArchiveShipmentStatusExternalBasicV2(
                altinnUsername, altinnUserPassword,
                SearchArchiveShipmentStatusBE()
            )
        } catch (e: IServiceOwnerArchiveExternalBasicTestAltinnFaultFaultFaultMessage) {
            logger.error {
                "Exception iArchiveAgencyExternalBasic.getArchiveShipmentStatusExternalBasicV2\n" +
                    "\n ErrorMessage  ${e.faultInfo.altinnErrorMessage}" +
                    "\n ExtendedErrorMessage  ${e.faultInfo.altinnExtendedErrorMessage}" +
                    "\n LocalizedErrorMessage  ${e.faultInfo.altinnLocalizedErrorMessage}" +
                    "\n ErrorGuid  ${e.faultInfo.errorGuid}" +
                    "\n UserGuid  ${e.faultInfo.userGuid}" +
                    "\n UserId  ${e.faultInfo.userId}"
            }
        }
    }
}

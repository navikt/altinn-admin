package no.nav.altinn.admin.service.notification

import mu.KotlinLogging
import no.altinn.schemas.services.serviceengine.standalonenotificationbe._2009._10.StandaloneNotificationBEList
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.dateToXmlGregorianCalendar
import no.nav.altinn.admin.common.isDate

private val logger = KotlinLogging.logger { }
class AltinnNotificationService(env: Environment, iNotificationAgencyExternalBasicFactory: () -> INotificationAgencyExternalBasic) {

    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iNotificationAgencyExternalBasic by lazy(iNotificationAgencyExternalBasicFactory)

    fun sendNotification(dateFrom: String, dateTo: String) {
        try {
            if (!isDate(dateFrom) || !isDate(dateTo))
                throw IllegalArgumentException("Wrong date format")

            val d1 = dateToXmlGregorianCalendar(dateFrom)
            val d2 = dateToXmlGregorianCalendar(dateTo)
            val receiptItems = iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(
                altinnUsername, altinnUserPassword,
                StandaloneNotificationBEList()
            )
        } catch (e: INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage) {
            logger.error {
                "Exception iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3\n" +
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

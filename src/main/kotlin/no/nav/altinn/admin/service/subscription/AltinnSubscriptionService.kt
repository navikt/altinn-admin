package no.nav.altinn.admin.service.subscription

import mu.KotlinLogging
import no.altinn.schemas.services.serviceengine.subscription._2009._10.SubscriptionDetails
import no.altinn.services.serviceengine.subscription._2009._10.ISubscriptionExternalBasic
import no.altinn.services.serviceengine.subscription._2009._10.ISubscriptionExternalBasicSubmitSubscriptionBasicAltinnFaultFaultFaultMessage
import no.nav.altinn.admin.Environment
import no.nav.altinn.admin.common.dateToXmlGregorianCalendar
import no.nav.altinn.admin.common.isDate

private val logger = KotlinLogging.logger { }
class AltinnSubscriptionService(env: Environment, iSubscriptionAgencyExternalBasicFactory: () -> ISubscriptionExternalBasic) {

    private val altinnUsername = env.altinn.username
    private val altinnUserPassword = env.altinn.password
    private val iSubscriptionExternalBasic by lazy(iSubscriptionAgencyExternalBasicFactory)

    fun submitSubscriptions(dateFrom: String, dateTo: String) {
        try {
            if (!isDate(dateFrom) || !isDate(dateTo))
                throw IllegalArgumentException("Wrong date format")

            val d1 = dateToXmlGregorianCalendar(dateFrom)
            val d2 = dateToXmlGregorianCalendar(dateTo)
            val receiptItems = iSubscriptionExternalBasic.submitSubscriptionBasic(
                altinnUsername, altinnUserPassword,
                "",
                SubscriptionDetails()
            )
        } catch (e: ISubscriptionExternalBasicSubmitSubscriptionBasicAltinnFaultFaultFaultMessage) {
            logger.error {
                "Exception iSubscriptionExternalBasic.submitSubscriptionBasic\n" +
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

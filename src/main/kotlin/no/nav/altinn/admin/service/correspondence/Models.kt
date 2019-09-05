package no.nav.altinn.admin.service.correspondence

import com.fasterxml.jackson.annotation.JsonProperty
import javax.xml.datatype.XMLGregorianCalendar

data class CorrespondenceResponse(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String,
    @JsonProperty("correspondenceDetails")
    val correspondenceDetails: List<CorrespondenceDetails>
)

data class CorrespondenceDetails(
    @JsonProperty("correspondenceID")
    val correspondenceID: Int = -1,
    @JsonProperty("createdDate")
    val createdDate: XMLGregorianCalendar? = null,
    @JsonProperty("reportee")
    val reportee: String = "",
    @JsonProperty("lastStatusChangeDate")
    val lastStatusChangeDate: XMLGregorianCalendar? = null,
    @JsonProperty("lastStatus")
    val lastStatus: String = ""
)
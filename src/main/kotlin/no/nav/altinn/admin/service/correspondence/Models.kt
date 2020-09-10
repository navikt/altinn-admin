package no.nav.altinn.admin.service.correspondence

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import javax.xml.datatype.XMLGregorianCalendar

data class CorrespondenceResponse(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String,
    @JsonProperty("correspondenceDetails")
    val correspondenceDetails: List<CorrespondenceDetails>
)

data class InsertCorrespondenceResponse(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String
)

data class PostCorrespondenceBody(
    @JsonProperty("tjenesteKode")
    val tjenesteKode: String,
    @JsonProperty("utgaveKode")
    val utgaveKode: String = "1",
    @JsonProperty("orgnr")
    val orgnr: String,
    @JsonProperty("melding")
    val melding: Melding,
    @JsonProperty("varseler")
    val varsel: List<Varsel>? = null,
    @JsonProperty("vedlegger")
    val vedlegger: List<Vedlegg>? = null
)

data class PostSpamCorrespondenceBody(
    @JsonProperty("tjenesteKode")
    val tjenesteKode: String,
    @JsonProperty("utgaveKode")
    val utgaveKode: String = "1",
    @JsonProperty("mottakere")
    val mottakere: List<String>,
    @JsonProperty("melding")
    val melding: Melding,
    @JsonProperty("varseler")
    val varsel: List<Varsel>? = null,
    @JsonProperty("vedlegger")
    val vedlegger: List<Vedlegg>? = null
)

data class Melding(
    @JsonProperty("tittel")
    val tittel: String,
    @JsonProperty("sammendrag")
    val sammendrag: String? = null,
    @JsonProperty("innhold")
    val innhold: String,
    @JsonProperty("tjenesteAttributter")
    val tjenesteAttributter: String? = null
)

data class Varsel(
    @JsonProperty("fraAdresse")
    val fraAdresse: String,
    @JsonProperty("forsendelseDatoTid")
    val forsendelseDatoTid: LocalDateTime,
    @JsonProperty("varselType")
    val varselType: VarselType,
    @JsonProperty("tittel")
    val tittel: String? = null,
    @JsonProperty("melding")
    val melding: String,
    @JsonProperty("ekstraMottakere")
    val ekstraMottakere: List<Mottaker>
)

data class Mottaker(
    @JsonProperty("forsendelseType")
    val forsendelseType: ForsendelseType,
    @JsonProperty("mottakerAdresse")
    val mottakerAdresse: String
)

enum class ForsendelseType { SMS, Email, Both }

enum class VarselType { TokenTextOnly, VarselDPVUtenRevarsel, VarselDPVMedRevarsel }

data class Vedlegg(
    @JsonProperty("filnavn")
    val filnavn: String,
    @JsonProperty("navn")
    val navn: String,
    @JsonProperty("dataString64")
    val data: String
)

data class CorrespondenceDetails(
    @JsonProperty("correspondenceID")
    val correspondenceID: Int = -1,
    @JsonProperty("createdDate")
    val createdDate: XMLGregorianCalendar? = null,
    @JsonProperty("reportee")
    val reportee: String = "",
    @JsonProperty("sendersReference")
    val sendersReference: String = "",
    @JsonProperty("notifications")
    val notifications: List<Notification>,
    @JsonProperty("lastStatusChangeDate")
    val lastStatusChangeDate: XMLGregorianCalendar? = null,
    @JsonProperty("lastStatus")
    val lastStatus: String = ""
)

data class Notification(
    @JsonProperty("transportType")
    val transportType: String,
    @JsonProperty("recipient")
    val recipient: String,
    @JsonProperty("sentDate")
    val sentDate: XMLGregorianCalendar? = null
)
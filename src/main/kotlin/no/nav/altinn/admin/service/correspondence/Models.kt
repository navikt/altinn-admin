package no.nav.altinn.admin.service.correspondence

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime
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

data class VedleggBody(
    @JsonProperty("requestBody")
    val requestBody: Content
)

data class Content(
    @JsonProperty("requestBody")
    val requestBody: String
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
    val forsendelseDatoTid: DateTime,
    @JsonProperty("forsendelseType")
    val forsendelseType: String,
    @JsonProperty("melding")
    val melding: String,
    @JsonProperty("ekstraMottakere")
    val ekstraMottakere: List<Mottaker>
)

data class Mottaker(
    @JsonProperty("mottakerType")
    val mottakerType: MottakerType,
    @JsonProperty("mottakerAdresse")
    val mottakerAdresse: String
)

enum class MottakerType { SMS, EPOST }

data class Vedlegg(
    @JsonProperty("filnavn")
    val filnavn: String,
    @JsonProperty("navn")
    val navn: String,
    @JsonProperty("data")
    val data: List<Byte>
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
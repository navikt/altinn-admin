package no.nav.altinn.admin.service.prefill

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class PrefillResponse(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String
)

data class PostPrefillBody(
    @JsonProperty("tjeneste")
    val tjeneste: PrefillType,
    @JsonProperty("orgnr")
    val orgnr: String,
    @JsonProperty("duedate")
    val duedate: String,
    @JsonProperty("melding")
    val melding: String,
    @JsonProperty("varsel")
    val varsel: List<Varsel>? = null
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

enum class PrefillType(val servicecode: String, val serviceeditioncode: String, val dataformatid: String, val dataformatversion: Int) {
    Test_IM("4765", "2", "5696", 42510);
}

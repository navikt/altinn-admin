package no.nav.altinn.admin.service.srr

import com.fasterxml.jackson.annotation.JsonProperty

data class PostLeggTilRettighetBody(
    @JsonProperty("tjenesteKode")
    val tjenesteKode: String,
    @JsonProperty("utgaveKode")
    val utgaveKode: String,
    @JsonProperty("orgnr")
    val orgnr: String,
    @JsonProperty("lesEllerSkriv")
    val lesEllerSkriv: String,
    @JsonProperty("domene")
    val domene: String
)

data class RightsResponse(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String
)

data class RightsResponseWithList(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String,
    val register: RegistryResponse
)

data class RegistryResponse(
    @JsonProperty("register")
    val register: List<Register>
) {
    data class Register(
        @JsonProperty("tjenesteOgUtgave")
        val tjenesteOgUtgave: String,
        @JsonProperty("virksomhetsnummer")
        val organisasjonsnummer: String,
        @JsonProperty("domene")
        val domene: String,
        @JsonProperty("rettighet")
        val rettighet: String,
        @JsonProperty("tilDato")
        val tilDato: String
    )
}

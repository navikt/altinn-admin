package no.nav.altinn.admin.service.srr

import com.fasterxml.jackson.annotation.JsonProperty

data class RequestRegister(
    @JsonProperty("organisasjonsnummer")
    val organisasjonsnummer: String,
    @JsonProperty("domene")
    val domene: String,
    @JsonProperty("rettighet")
    val rettighet: String
)

data class RightsResponse(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String
)

data class RegistryResponse(
    @JsonProperty("register")
    val register: List<Register>
) {
    data class Register(
        @JsonProperty("organisasjonsnummer")
        val organisasjonsnummer: String,
        @JsonProperty("domene")
        val domene: String,
        @JsonProperty("rettighet")
        val rettighet: String,
        @JsonProperty("tilDato")
        val tilDato: String
    )
}

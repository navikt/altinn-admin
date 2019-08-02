package no.nav.altinn.admin.service.dq

import com.fasterxml.jackson.annotation.JsonProperty

data class DqResponseFormData(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String,
    @JsonProperty("formData")
    val formData: FormData
)

data class DqItems(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("items")
    val items: List<DqItem>
)

data class DqItem(
    @JsonProperty("archiveReference")
    val archiveReference: String
)

data class Attachments(
    @JsonProperty("attachments")
    val attachments: List<Attachment>
)

data class Attachment(
    @JsonProperty("filename")
    val filename: String,
    @JsonProperty("index")
    val index: Int,
    @JsonProperty("dataBase64")
    val dataBase64: String,
    @JsonProperty("fileSize")
    val fileSize: Int,
    @JsonProperty("encrypted")
    val encrypted: Boolean,
    @JsonProperty("type")
    val type: String
)

data class DqPurge(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String
)

data class FormData(
    @JsonProperty("formData")
    val formData: String,
    @JsonProperty("attachments")
    val attachments: Attachments?
)
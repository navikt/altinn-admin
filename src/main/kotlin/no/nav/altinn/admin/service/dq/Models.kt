package no.nav.altinn.admin.service.dq

import com.fasterxml.jackson.annotation.JsonProperty

data class DqResponseFormTask(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("message")
    val message: String,
    @JsonProperty("arData")
    val arData: ArData
)

data class DqItems(
    @JsonProperty("status")
    val status: String,
    @JsonProperty("size")
    val size: Int,
    @JsonProperty("items")
    val items: List<DqItem>
)

data class DqItem(
    @JsonProperty("archiveReference")
    val archiveReference: String,
    @JsonProperty("serviceCode")
    val serviceCode: String,
    @JsonProperty("serviceEditionCode")
    val serviceEditionCode: String
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

data class ArData(
    @JsonProperty("archiveReference")
    val archiveReference: String = "",
    @JsonProperty("archiveTimeStamp")
    val archiveTimeStamp: String = "",
    @JsonProperty("serviceCode")
    val serviceCode: String = "",
    @JsonProperty("serviceEditionCode")
    val serviceEditionCode: String = "",
    @JsonProperty("dataFormatId")
    val dataFormatId: String = "",
    @JsonProperty("dataFormatVersion")
    val dataFormatVersion: Int = -1,
    @JsonProperty("numberOfForms")
    val numberOfForms: Int = 0,
    @JsonProperty("numberOfAttachments")
    val numberOfAttachments: Int = 0,
    @JsonProperty("formData")
    val formData: String = "",
    @JsonProperty("attachments")
    val attachments: Attachments? = null
)
package no.nav.altinn.admin.common

import org.slf4j.MDC
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

private class Common

internal fun getStringFromResource(path: String) =
    Common::class.java.getResourceAsStream(path).bufferedReader().use { it.readText() }

internal fun randomUuid() = UUID.randomUUID().toString()
internal fun decodeBase64(s: String): ByteArray = Base64.getDecoder().decode(s)
internal fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
internal fun getCorrelationId(): CorrelationId =
    CorrelationId(MDC.get(MDC_CALL_ID))

internal fun getCurrentDate(formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE) =
    LocalDateTime.now().format(formatter)

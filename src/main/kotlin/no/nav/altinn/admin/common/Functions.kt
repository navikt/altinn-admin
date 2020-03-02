package no.nav.altinn.admin.common

import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

internal fun randomUuid() = UUID.randomUUID().toString()
internal fun decodeBase64(s: String): ByteArray = Base64.getDecoder().decode(s)
fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
fun isDate(date: String): Boolean {
    val reg = """\d{4}-\d{2}-\d{2}""".toRegex()
    return date.matches(reg)
}
fun toXmlGregorianCalendar(date: String): XMLGregorianCalendar {
    val ld = LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val dtf = DatatypeFactory.newInstance()
    return dtf.newXMLGregorianCalendar(GregorianCalendar.from(ld.atStartOfDay(ZoneId.systemDefault())))
}
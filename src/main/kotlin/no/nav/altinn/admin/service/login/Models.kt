package no.nav.altinn.admin.service.login

data class UserSession(val token: String)

data class UserInfo(
    val aio: String?,
    val amr: List<String>,
    val family_name: String,
    val given_name: String,
    val ipaddr: String,
    val name: String,
    val oid: String,
    val onprem_sid: String,
    val rh: String,
    val sub: String,
    val tid: String,
    val unique_name: String,
    val upn: String,
    val uti: String?,
    val ver: String,
    val groups: List<String>
)

data class LoginInfo(
    val info: String,
    val token: String
)

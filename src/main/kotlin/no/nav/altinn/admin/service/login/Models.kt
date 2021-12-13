package no.nav.altinn.admin.service.login

data class UserInfo(
    val family_name: String,
    val given_name: String,
    val ipaddr: String,
    val name: String,
    val unique_name: String,
    val upn: String,
    val groups: List<String>
)

data class UserSession(
    val idToken: String
)

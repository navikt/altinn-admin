package no.nav.altinn.admin.common


data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

data class AuthenticatedUser(
        val identifier: String,
        val email: String,
        val firstName: String,
        val lastName: String
) {
    val displayName: String
        get() = "$lastName, $firstName"

    override fun toString(): String = "$lastName, $firstName ($identifier - $email)"
}

inline class CorrelationId(val id: String) {
    override fun toString(): String = id
}
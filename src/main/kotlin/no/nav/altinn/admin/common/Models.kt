package no.nav.altinn.admin.common

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

inline class CorrelationId(val id: String) {
    override fun toString(): String = id
}
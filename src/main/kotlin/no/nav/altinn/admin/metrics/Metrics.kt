package no.nav.altinn.admin.metrics

import io.prometheus.client.Counter

private const val NAMESPACE = "altinn_admin"

object Metrics {
    val srrExipingRules: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("expiring_rettighetsregler")
        .labelNames("sc")
        .help("Antall regler i rettighetsregister som er utgående")
        .register()
}

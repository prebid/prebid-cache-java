package org.prebid.cache.functional.model.request

data class MetricDetail(
    val name: String,
    val description: String? = null,
    val baseUnit: String? = null,
    val measurements: List<Measurement>,
    val availableTags: List<AvailableTag>
)

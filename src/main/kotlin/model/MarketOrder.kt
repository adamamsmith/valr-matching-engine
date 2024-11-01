package smith.adam.model

import kotlinx.serialization.Serializable

@Serializable
data class MarketOrder(
    val id: String? = null,
    val side: String,
    val baseAmount: Double? = null,
    val quoteAmount: Double? = null,
    val pair: String
)

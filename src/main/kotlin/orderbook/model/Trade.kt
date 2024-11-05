package smith.adam.orderbook.model

import kotlinx.serialization.Serializable

@Serializable
data class Trade(
    val id: String,
    val price: Double,
    val quantity: Double,
    val currencyPair: String,
    val tradedAt: String,
    val takerSide: String,
    val sequenceId: Long,
    val quoteVolume: Double
)
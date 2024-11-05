package smith.adam.orderbook.model

import kotlinx.serialization.Serializable

@Serializable
data class MarketOrder(
    override val id: String? = null,
    override val side: String,
    override val baseAmount: Double? = null,
    override val quoteAmount: Double? = null,
    override val pair: String
) : BaseOrder(id, side, null, baseAmount, quoteAmount, pair)

package smith.adam.orderbook.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class MarketOrder(
    override val id: String? = null,
    override val side: String,
    override val baseAmount: Double? = null,
    override val quoteAmount: Double? = null,
    override val pair: String
) : BaseOrder() {
    @Transient override val price: Double? = null
}

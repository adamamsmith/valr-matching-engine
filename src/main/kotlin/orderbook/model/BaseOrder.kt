package smith.adam.orderbook.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class BaseOrder(
    @Transient open val id: String? = null,
    @Transient open val side: String = "",
    @Transient open val price: Double? = null,
    @Transient open val baseAmount: Double? = null,
    @Transient open val quoteAmount: Double? = null,
    @Transient open val pair: String = ""
)
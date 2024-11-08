package smith.adam.orderbook.model

import kotlinx.serialization.Serializable

@Serializable
data class CancelOrder(
    val orderId: String,
    val pair: String,
)
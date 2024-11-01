package smith.adam.model

import kotlinx.serialization.Serializable

@Serializable
data class CancelOrder(
    val orderId: String,
    val pair: String,
)
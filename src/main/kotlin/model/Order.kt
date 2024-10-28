package smith.adam.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String? = null, // Make ID optional
    val type: String, // "market" or "limit"
    val price: Double? = null, // Only for limit orders
    val quantity: Int
)

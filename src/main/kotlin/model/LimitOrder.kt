package smith.adam.model

import kotlinx.serialization.Serializable

@Serializable
data class LimitOrder(
    val id: String? = null,
    val side: String,
    val quantity: Double,
    val price: Double,
    val pair: String,
    val timestamp: Long = System.currentTimeMillis()
)


package smith.adam.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderBook(
    val buyOrders: MutableList<Order> = mutableListOf(),
    val sellOrders: MutableList<Order> = mutableListOf()
) {
    fun isEmpty(): Boolean {
        return buyOrders.isEmpty() && sellOrders.isEmpty()
    }
}
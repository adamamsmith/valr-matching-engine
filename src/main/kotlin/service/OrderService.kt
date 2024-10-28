package smith.adam.service

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import smith.adam.model.Order
import smith.adam.model.OrderBook
import smith.adam.model.OrderEvent

class OrderService {
    private val orderBook = OrderBook()
    private val eventChannel = Channel<OrderEvent>(Channel.UNLIMITED)

    init {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            eventChannel.consumeEach { event ->
                when (event) {
                    is OrderEvent.CreateOrder -> addOrder(event.order)
                    is OrderEvent.DeleteOrder -> removeOrder(event.id)
                }
            }
        }
    }

    fun placeOrder(order: Order): String {
        val orderId = "order-${System.currentTimeMillis()}"
        eventChannel.trySend(OrderEvent.CreateOrder(order.copy(id = orderId)))
        return orderId
    }

    fun cancelOrder(orderId: String): Boolean {
        eventChannel.trySend(OrderEvent.DeleteOrder(orderId))
        return true
    }

    fun getOrderBook(): OrderBook {
        return orderBook
    }

    private fun addOrder(order: Order) {
        if (order.type == "buy") {
            orderBook.buyOrders.add(order)
        } else {
            orderBook.sellOrders.add(order)
        }
    }

    private fun removeOrder(id: String) {
        orderBook.buyOrders.removeIf { it.id == id }
        orderBook.sellOrders.removeIf { it.id == id }
    }
}

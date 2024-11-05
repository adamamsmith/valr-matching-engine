package smith.adam.orderbook

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import smith.adam.orderbook.model.*

abstract class BaseOrderBook(val pair: String, val decimals: Int) {
    private var sequenceId: Long = 0
    private var orderId: Long = 0
    protected val tradeHistory: List<Trade> = mutableListOf()

    private val eventChannel = Channel<OrderEvent>(Channel.UNLIMITED)

    // TODO: Understand this better
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + job)

    init {
        coroutineScope.launch {
            eventChannel.consumeEach { event ->
                when (event) {
                    is OrderEvent.CreateLimitOrder -> add(event.limitOrder)
                    is OrderEvent.CreateMarketOrder -> match(event.marketOrder)
                    is OrderEvent.CancelLimitOrder -> remove(event.cancelOrder.orderId)
                }
            }
        }
    }

    protected fun getAndIncrementSequenceId(): Long {
        val current = sequenceId
        sequenceId++
        return current
    }

    private fun getAndIncrementOrderId(): Long {
        val current = orderId
        orderId++
        return current
    }

    fun placeMarketOrder(marketOrder: MarketOrder): String {
        val orderId = "${getAndIncrementOrderId()}-0000"
        eventChannel.trySend(OrderEvent.CreateMarketOrder(marketOrder.copy(id = orderId)))
        return orderId
    }

    fun placeLimitOrder(limitOrder: LimitOrder): String {
        val orderId = "${getAndIncrementOrderId()}-0001"
        eventChannel.trySend(OrderEvent.CreateLimitOrder(limitOrder.copy(id = orderId)))
        return orderId
    }

    fun cancelLimitOrder(cancelOrder: CancelOrder): String {
        eventChannel.trySend(OrderEvent.CancelLimitOrder(cancelOrder))
        return cancelOrder.orderId
    }

    abstract fun getBook(): Map<String, List<LimitOrder>>

    abstract fun getTradeHistory(offset: Int, limit: Int): List<Trade>

    protected abstract fun add(limitOrder: LimitOrder)

    protected abstract fun match(order: BaseOrder): Double

    protected abstract fun remove(orderId: String): Boolean
}
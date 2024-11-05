package smith.adam.orderbook

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import smith.adam.orderbook.model.*
import java.util.*

abstract class BaseOrderBook(val pair: String, val decimals: Int) {
    private var sequenceId: Long = 0
    private var orderIdSeed: Long = 0
    protected val tradeHistory: MutableList<Trade> = mutableListOf()

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

    private fun getAndIncrementSequenceId(): Long {
        val current = sequenceId
        sequenceId++
        return current
    }

    private fun constructOrderId(): String {
        val current = orderIdSeed
        orderIdSeed++
        return UUID.nameUUIDFromBytes(byteArrayOf(current.toByte())).toString()
    }

    fun placeMarketOrder(marketOrder: MarketOrder): String {
        val orderId = constructOrderId()
        eventChannel.trySend(OrderEvent.CreateMarketOrder(marketOrder.copy(id = orderId)))
        return orderId
    }

    fun placeLimitOrder(limitOrder: LimitOrder): String {
        val orderId = constructOrderId()
        eventChannel.trySend(OrderEvent.CreateLimitOrder(limitOrder.copy(id = orderId)))
        return orderId
    }

    fun cancelLimitOrder(cancelOrder: CancelOrder): String {
        eventChannel.trySend(OrderEvent.CancelLimitOrder(cancelOrder))
        return cancelOrder.orderId
    }

    protected fun addTrade(
        order: BaseOrder,
        weightedAveragePrice: Double,
        totalOrderAmount: Double,
        remainingQuantity: Double
    ) {
        val trade = Trade(
            id = "${order.id}",
            currencyPair = order.pair,
            price = weightedAveragePrice,
            quantity = totalOrderAmount - remainingQuantity,
            tradedAt = "${System.currentTimeMillis()}",
            takerSide = "BUY",
            sequenceId = getAndIncrementSequenceId(),
            quoteVolume = totalOrderAmount - remainingQuantity
        )
        tradeHistory.addFirst(trade)
    }

    abstract fun getBook(): Map<String, List<LimitOrder>>

    abstract fun getTradeHistory(offset: Int, limit: Int): List<Trade>

    protected abstract fun add(limitOrder: LimitOrder)

    protected abstract fun match(order: BaseOrder): Double

    protected abstract fun remove(orderId: String): Boolean
}
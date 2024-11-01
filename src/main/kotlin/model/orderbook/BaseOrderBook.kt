package smith.adam.model.orderbook

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import smith.adam.model.CancelOrder
import smith.adam.model.LimitOrder
import smith.adam.model.MarketOrder
import smith.adam.model.Trade
import smith.adam.model.orderbook.model.OrderEvent

abstract class BaseOrderBook {
    private var sequenceId: Long = 0
    protected val tradeHistory: List<Trade> = mutableListOf()

    private val eventChannel = Channel<OrderEvent>(Channel.UNLIMITED)

    init {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            eventChannel.consumeEach { event ->
                when (event) {
                    is OrderEvent.CreateLimitOrder -> add(event.limitOrder)
                    is OrderEvent.CreateMarketOrder -> execute(event.marketOrder)
                }
            }
        }
    }

    protected fun getAndIncrementSequenceId(): Long {
        val current = sequenceId
        sequenceId++
        return current
    }

    fun placeMarketOrder(marketOrder: MarketOrder): String {
        val orderId = "${System.currentTimeMillis()}-0000"
        eventChannel.trySend(OrderEvent.CreateMarketOrder(marketOrder.copy(id = orderId)))
        return orderId
    }

    fun placeLimitOrder(limitOrder: LimitOrder): String {
        val orderId = "${System.currentTimeMillis()}-0001"
        eventChannel.trySend(OrderEvent.CreateLimitOrder(limitOrder.copy(id = orderId)))
        return orderId
    }
    
    fun cancelLimitOrder(cancelOrder: CancelOrder): Boolean {
        return remove(cancelOrder)
    }

    abstract fun getBook(): Map<String, List<LimitOrder>>

    abstract fun getTradeHistory(offset: Int, limit: Int): List<Trade>

    protected abstract fun add(limitOrder: LimitOrder)

    protected abstract fun execute(marketOrder: MarketOrder)

    protected abstract fun remove(cancelOrder: CancelOrder): Boolean
}
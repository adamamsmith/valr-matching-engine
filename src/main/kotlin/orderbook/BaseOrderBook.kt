package smith.adam.orderbook

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import smith.adam.orderbook.model.*
import java.util.concurrent.atomic.AtomicLong

abstract class BaseOrderBook(val pair: String, val decimals: Int) : AbstractVerticle() {
    private val sequenceId = AtomicLong(0)
    protected val tradeHistory: MutableList<Trade> = mutableListOf()

    override fun start() {
        val eventBus: EventBus = vertx.eventBus()
        eventBus.consumer(OrderRequest.address(pair)) { message -> handleOrderEvent(message) }

        eventBus.consumer<GetBookRequest>(GetBookRequest.address(pair)) { message ->
            val orderBook = getBook()
            message.reply(GetBookResponse(orderBook))
        }

        eventBus.consumer<GetTradeHistoryRequest>(GetTradeHistoryRequest.address(pair)) { message ->
            val request = message.body()
            val tradeHistory = getTradeHistory(request.offset, request.limit)
            message.reply(GetTradeHistoryResponse(tradeHistory))
        }
    }

    private fun handleOrderEvent(message: Message<OrderRequest>) {
        try {
            when (val event = message.body()) {
                is OrderRequest.CreateLimitOrder -> add(event.limitOrder)
                is OrderRequest.CreateMarketOrder -> match(event.marketOrder)
                is OrderRequest.CancelLimitOrder -> {
                    val result = remove(event.cancelOrder.orderId)
                    message.reply(result)
                }
            }
        } catch (e: Exception) {
            message.fail(500, "Error processing order event: ${e.message}")
        }
    }

    abstract fun getBook(): Map<String, List<LimitOrder>>

    abstract fun getTradeHistory(offset: Int, limit: Int): List<Trade>

    protected abstract fun add(limitOrder: LimitOrder)

    protected abstract fun match(order: BaseOrder): Double

    protected abstract fun remove(orderId: String): Boolean

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
            sequenceId = sequenceId.getAndIncrement(),
            quoteVolume = totalOrderAmount - remainingQuantity
        )
        tradeHistory.addFirst(trade)
    }
}
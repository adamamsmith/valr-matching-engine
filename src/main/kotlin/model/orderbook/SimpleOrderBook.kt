package smith.adam.model.orderbook

import smith.adam.model.LimitOrder
import smith.adam.model.MarketOrder
import smith.adam.model.Trade
import smith.adam.model.orderbook.model.Side
import java.util.*

class SimpleOrderBook : BaseOrderBook() {
    private val bids: TreeSet<LimitOrder> =
        TreeSet<LimitOrder>(compareByDescending<LimitOrder> { it.price }.thenBy { it.timestamp })
    private val asks: TreeSet<LimitOrder> =
        TreeSet<LimitOrder>(compareBy<LimitOrder> { it.price }.thenBy { it.timestamp })

    override fun getBook(): Map<String, List<LimitOrder>> {
        return mapOf(
            "Bids" to bids.toList(),
            "Asks" to asks.toList()
        )
    }

    override fun getTradeHistory(offset: Int, limit: Int): List<Trade> {
        return tradeHistory
            .drop(offset)
            .take(limit)
    }

    override fun add(limitOrder: LimitOrder) {
        if (limitOrder.side == "BUY") {
            bids.add(limitOrder)
        } else {
            asks.add(limitOrder)
        }
    }

    override fun remove(orderId: String): Boolean {
        val bidsRemoved = bids.removeIf { it.id == orderId }
        val asksRemoved = asks.removeIf { it.id == orderId }

        return bidsRemoved || asksRemoved
    }

    override fun execute(marketOrder: MarketOrder) {
        val isBuyOrder = Side.fromString(marketOrder.side) == Side.BUY
        val orders = if (isBuyOrder) asks else bids
        val totalOrderAmount = if (isBuyOrder) marketOrder.quoteAmount!! else marketOrder.baseAmount!!

        var remainingQuantity = totalOrderAmount
        var weightedAveragePrice = 0.0

        val it: MutableIterator<LimitOrder> = orders.iterator()
        while (it.hasNext()) {
            val order = it.next()

            if (remainingQuantity <= 0) break
            val tradeQuantity = minOf(remainingQuantity, order.quantity)

            remainingQuantity -= tradeQuantity
            weightedAveragePrice += order.price * tradeQuantity

            if (order.quantity > tradeQuantity) {
                val updatedOrder = order.copy(quantity = order.quantity - tradeQuantity)
                it.remove()
                orders.add(updatedOrder)
            } else {
                it.remove()
            }
        }

        weightedAveragePrice /= (totalOrderAmount - remainingQuantity)

        val trade = Trade(
            id = "${System.currentTimeMillis()}-0002",
            currencyPair = marketOrder.pair,
            price = weightedAveragePrice,
            quantity = totalOrderAmount - remainingQuantity,
            tradedAt = "${System.currentTimeMillis()}",
            takerSide = "BUY",
            sequenceId = getAndIncrementSequenceId(),
            quoteVolume = totalOrderAmount - remainingQuantity
        )
        (tradeHistory as MutableList).addFirst(trade)
    }
}
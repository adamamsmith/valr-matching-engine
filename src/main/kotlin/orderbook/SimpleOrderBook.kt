package smith.adam.orderbook

import smith.adam.orderbook.model.BaseOrder
import smith.adam.orderbook.model.LimitOrder
import smith.adam.orderbook.model.Trade
import smith.adam.orderbook.model.Side
import java.util.*

class SimpleOrderBook(pair: String, decimals: Int) : BaseOrderBook(pair, decimals) {
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

    override fun match(order: BaseOrder): Double {
        val isBuyOrder = Side.fromString(order.side) == Side.BUY
        val orders = if (isBuyOrder) asks else bids
        val totalOrderAmount = if (isBuyOrder) order.quoteAmount!! else order.baseAmount!!

        var remainingQuantity = totalOrderAmount
        var weightedAveragePrice = 0.0

        val limitOrderIt: MutableIterator<LimitOrder> = orders.iterator()
        while (limitOrderIt.hasNext()) {
            val limitOrder = limitOrderIt.next()

            if (remainingQuantity <= 0) break
            if (order.price != null) {
                if ((isBuyOrder && order.price!! < limitOrder.price)
                    || (!isBuyOrder && order.price!! > limitOrder.price)
                ) break
            }
            val tradeQuantity = minOf(remainingQuantity, limitOrder.quantity)

            remainingQuantity -= tradeQuantity
            weightedAveragePrice += limitOrder.price * tradeQuantity

            if (limitOrder.quantity > tradeQuantity) {
                val updatedOrder = limitOrder.copy(quantity = limitOrder.quantity - tradeQuantity)
                limitOrderIt.remove()
                orders.add(updatedOrder)
            } else {
                limitOrderIt.remove()
            }
        }

        weightedAveragePrice /= (totalOrderAmount - remainingQuantity)

        val trade = Trade(
            id = "${System.currentTimeMillis()}-0002",
            currencyPair = order.pair,
            price = weightedAveragePrice,
            quantity = totalOrderAmount - remainingQuantity,
            tradedAt = "${System.currentTimeMillis()}",
            takerSide = "BUY",
            sequenceId = getAndIncrementSequenceId(),
            quoteVolume = totalOrderAmount - remainingQuantity
        )
        (tradeHistory as MutableList).addFirst(trade)
        return remainingQuantity
    }
}
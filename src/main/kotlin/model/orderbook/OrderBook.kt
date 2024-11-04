package smith.adam.model.orderbook

import smith.adam.model.LimitOrder
import smith.adam.model.MarketOrder
import smith.adam.model.Trade
import smith.adam.model.orderbook.model.BookOrder
import smith.adam.model.orderbook.model.Level
import smith.adam.model.orderbook.model.Side
import smith.adam.model.orderbook.tree.RedBlackTree

class OrderBook : BaseOrderBook() {
    private var bids: RedBlackTree<Level> = RedBlackTree()
    private var asks: RedBlackTree<Level> = RedBlackTree()
    private var bestBid: RedBlackTree.Node<Level>? = null
    private var bestAsk: RedBlackTree.Node<Level>? = null

    private var levelNodes: MutableMap<Double, RedBlackTree.Node<Level>> = mutableMapOf()
    private var orders: MutableMap<String, BookOrder> = mutableMapOf()

    override fun getBook(): Map<String, List<LimitOrder>> {
        return mapOf(
            "Bids" to bids.toList { it.orders() },
            "Asks" to asks.toList { it.orders() }
        )
    }

    override fun getTradeHistory(offset: Int, limit: Int): List<Trade> {
        return tradeHistory
            .drop(offset)
            .take(limit)
    }

    override fun add(limitOrder: LimitOrder) {
        if (limitOrder.price !in levelNodes) {
            when (Side.fromString(limitOrder.side)) {
                Side.BUY -> {
                    val node = bids.add(limitOrder.toLevel())

                    if (bestBid == null || bestBid!!.data > node.data) {
                        bestBid = node
                    }

                    levelNodes[node.data.price] = node
                }

                Side.SELL -> {
                    val node = asks.add(limitOrder.toLevel())

                    if (bestAsk == null || bestAsk!!.data < node.data) {
                        bestAsk = node
                    }

                    levelNodes[node.data.price] = node
                }

                else -> throw Exception("Invalid side")
            }
        }

        val node = levelNodes[limitOrder.price]!!
        val tailOrder = node.data.tailOrder
        val order = limitOrder.toBookOrder(nextOrder = null, previousOrder = tailOrder, parent = node)

        if (tailOrder == null) node.data.headOrder = order else tailOrder.nextOrder = order

        node.data.tailOrder = order
        node.data.size += 1
        node.data.totalQuantity += order.quantity

        orders[order.id!!] = order
    }

    override fun execute(marketOrder: MarketOrder) {
        val isBuyOrder = Side.fromString(marketOrder.side) == Side.BUY
        val bestLevel = if (isBuyOrder) bestAsk else bestBid
        val levels = if (isBuyOrder) asks else bids
        val totalOrderAmount = if (isBuyOrder) marketOrder.quoteAmount!! else marketOrder.baseAmount!!

        var remainingQuantity = totalOrderAmount
        var weightedAveragePrice = 0.0

        val it = levels.iteratorFromNode(bestLevel)
        while (it.hasNext()) {
            val node = it.next()

            if (remainingQuantity <= 0) break
            val tradeQuantity = minOf(remainingQuantity, node.data.totalQuantity)

            remainingQuantity -= tradeQuantity
            weightedAveragePrice += node.data.price * tradeQuantity

            if (node.data.totalQuantity > tradeQuantity) {
                var remainingOrderQuantity = tradeQuantity
                val orderIt = node.data.orderIterator()
                while (orderIt.hasNext()) {
                    val order = orderIt.next()

                    if (remainingQuantity <= 0) break
                    val matchedQuantity = minOf(remainingOrderQuantity, order.quantity)
                    remainingOrderQuantity -= order.quantity

                    if (order.quantity > matchedQuantity) {
                        order.quantity -= matchedQuantity
                    } else {
                        remove(order.id!!)
                    }
                }

            } else {
                deleteLevel(node, price = node.data.price)
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

    override fun remove(orderId: String): Boolean {
        val order = orders[orderId] ?: return false
        val node = levelNodes[order.price]!!

        if (node.data.size == 1) {
            deleteLevel(level = node, price = order.price)
            return true
        }

        node.data.size -= 1
        node.data.totalQuantity -= order.quantity
        if (node.data.headOrder?.id == order.id) {
            node.data.headOrder = order.nextOrder
        }
        if (node.data.tailOrder?.id == order.id) {
            node.data.tailOrder = order.previousOrder
        }

        order.previousOrder?.nextOrder = order.nextOrder
        order.nextOrder?.previousOrder = order.previousOrder
        orders.remove(orderId)

        return true
    }

    private fun deleteLevel(level: RedBlackTree.Node<Level>, price: Double) {
        for (order in level.data.orders()) {
            orders.remove(order.id)
        }

        when (level.data.side) {
            Side.BUY -> {
                if (bestBid == level) {
                    bestBid = level.parent
                }

                val deletedNode = bids.delete(level)
                // This handles the case of when deleting a node with two children the node.data is overridden with a
                // deletionNode.data and then the deletion node is the actual node to be deleted, so the levelNodes
                // map has to be updated.
                if (deletedNode.data.price != price) {
                    levelNodes[deletedNode.data.price] = level
                }
            }

            Side.SELL -> {
                if (bestAsk == level) {
                    bestAsk = level.parent
                }

                val deletedNode = asks.delete(level)
                // This is again handling of the special case. See above comment on BUY side.
                if (deletedNode.data.price != price) {
                    levelNodes[deletedNode.data.price] = level
                }
            }
        }
        levelNodes.remove(price)
    }
}
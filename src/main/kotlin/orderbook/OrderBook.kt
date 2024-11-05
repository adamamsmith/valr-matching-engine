package smith.adam.orderbook

import smith.adam.orderbook.model.BaseOrder
import smith.adam.orderbook.model.LimitOrder
import smith.adam.orderbook.model.Trade
import smith.adam.orderbook.model.Level
import smith.adam.orderbook.model.Side
import smith.adam.orderbook.tree.RedBlackTree

class OrderBook(pair: String, decimals: Int) : BaseOrderBook(pair, decimals) {
    private var bids: RedBlackTree<Level> = RedBlackTree()
    private var asks: RedBlackTree<Level> = RedBlackTree()
    private var bestBid: RedBlackTree.Node<Level>? = null
    private var bestAsk: RedBlackTree.Node<Level>? = null

    private var levelNodes: MutableMap<Double, RedBlackTree.Node<Level>> = mutableMapOf()
    private var orders: MutableMap<String, LimitOrder> = mutableMapOf()

    override fun getBook(): Map<String, List<LimitOrder>> {
        return mapOf(
            "Bids" to bids.toList({ it.orders() }, true),
            "Asks" to asks.toList({ it.orders() }, false)
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
        val order = limitOrder.copy(nextOrder = null, previousOrder = tailOrder, parent = node)

        if (tailOrder == null) node.data.headOrder = order else tailOrder.nextOrder = order

        node.data.tailOrder = order
        node.data.size += 1
        node.data.totalQuantity += order.quantity

        orders[order.id!!] = order
    }

    override fun match(order: BaseOrder): Double {
        val isBuyOrder = Side.fromString(order.side) == Side.BUY
        val bestLevel = if (isBuyOrder) bestAsk else bestBid
        val levels = if (isBuyOrder) asks else bids
        val totalOrderAmount = if (isBuyOrder) order.quoteAmount!! else order.baseAmount!!

        var remainingQuantity = totalOrderAmount
        var weightedAveragePrice = 0.0

        val it = levels.iteratorFromNode(bestLevel)
        while (it.hasNext()) {
            val node = it.next()

            if (remainingQuantity <= 0) break
            if (order.price != null) {
                if ((isBuyOrder && order.price!! < node.data.price)
                    || (!isBuyOrder && order.price!! > node.data.price)
                ) break
            }

            val tradeQuantity = minOf(remainingQuantity, node.data.totalQuantity)

            remainingQuantity -= tradeQuantity
            weightedAveragePrice += node.data.price * tradeQuantity

            if (node.data.totalQuantity > tradeQuantity) {
                var remainingOrderQuantity = tradeQuantity
                val limitOrderIt = node.data.orderIterator()
                while (limitOrderIt.hasNext()) {
                    val limitOrder = limitOrderIt.next()

                    if (remainingQuantity <= 0) break
                    val matchedQuantity = minOf(remainingOrderQuantity, limitOrder.quantity)
                    remainingOrderQuantity -= limitOrder.quantity

                    if (limitOrder.quantity > matchedQuantity) {
                        limitOrder.quantity -= matchedQuantity
                    } else {
                        remove(limitOrder.id!!)
                    }
                }

            } else {
                deleteLevel(node, price = node.data.price)
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
package smith.adam.orderbook

import smith.adam.orderbook.model.BaseOrder
import smith.adam.orderbook.model.Level
import smith.adam.orderbook.model.LimitOrder
import smith.adam.orderbook.model.Side
import smith.adam.orderbook.tree.RedBlackTree

class OrderBook(pair: String) : BaseOrderBook(pair) {
    private var bids: RedBlackTree<Level> = RedBlackTree()
    private var asks: RedBlackTree<Level> = RedBlackTree()
    private var bestBid: RedBlackTree.Node<Level>? = null
    private var bestAsk: RedBlackTree.Node<Level>? = null

    private var levelNodes: MutableMap<Double, RedBlackTree.Node<Level>> = mutableMapOf()
    private var orders: MutableMap<String, LimitOrder> = mutableMapOf()

    override fun getBook(): Map<String, List<LimitOrder>> {
        return mapOf("Bids" to bids.toList(true) { it.orders() }, "Asks" to asks.toList { it.orders() })
    }

    override fun add(limitOrder: LimitOrder) {
        if (crossingBidAsk(limitOrder)) limitOrder.quantity = match(limitOrder)
        if (limitOrder.quantity <= 0) return

        if (limitOrder.price !in levelNodes) {
            when (Side.fromString(limitOrder.side)) {
                Side.BUY -> {
                    val node = bids.add(limitOrder.toLevel())

                    if (bestBid == null || node.data > bestBid!!.data) {
                        bestBid = node
                    }

                    levelNodes[node.data.price] = node
                }

                Side.SELL -> {
                    val node = asks.add(limitOrder.toLevel())

                    if (bestAsk == null || node.data < bestAsk!!.data) {
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
        node.data.baseAmount += order.quantity
        node.data.quoteAmount += order.quantity * order.price

        orders[order.id!!] = order
    }

    override fun match(order: BaseOrder): Double {
        if (order.quoteAmount != null) {
            // Market BUY; Limit orders & Market SELL always have quoteAmount = null
            var remainingQuoteAmount = order.quoteAmount!!
            var tradedBaseAmount = 0.0
            var weightedAveragePrice = 0.0

            val it = asks.iteratorFromNode(bestAsk)
            while (it.hasNext()) {
                val node = it.next()

                if (remainingQuoteAmount <= 0) break

                val quoteAmount = minOf(remainingQuoteAmount, node.data.quoteAmount)
                val baseAmount = minOf(quoteAmount / node.data.price, node.data.baseAmount)

                remainingQuoteAmount -= quoteAmount
                tradedBaseAmount += baseAmount
                weightedAveragePrice += quoteAmount

                if (node.data.quoteAmount > quoteAmount) {
                    matchOrdersInLevel(node.data, baseAmount)
                } else {
                    deleteLevel(node)
                }
            }

            weightedAveragePrice /= tradedBaseAmount
            addTrade(order, weightedAveragePrice, tradedBaseAmount)

            return 0.0
        }
        else {
            // Market SELL or Limit order placement crossing bid ask spread
            val isSELL = Side.fromString(order.side) == Side.SELL
            val bookSide = if (isSELL) bids else asks
            val bestLevel = if (isSELL) bestBid else bestAsk

            var remainingBaseAmount = order.baseAmount!!
            var weightedAveragePrice = 0.0

            val it = bookSide.iteratorFromNode(bestLevel, reverse = isSELL)
            while (it.hasNext()) {
                val node = it.next()

                if (remainingBaseAmount <= 0) break
                if (isSELL) {
                    if (order.price != null && order.price!! > node.data.price) break
                } else {
                    if (order.price != null && order.price!! < node.data.price) break
                }

                val baseAmount = minOf(remainingBaseAmount, node.data.baseAmount)

                remainingBaseAmount -= baseAmount
                weightedAveragePrice += baseAmount * node.data.price

                if (node.data.baseAmount > baseAmount) {
                    matchOrdersInLevel(node.data, baseAmount)
                } else {
                    deleteLevel(node)
                }
            }

            weightedAveragePrice /= (order.baseAmount!! - remainingBaseAmount)
            addTrade(order, weightedAveragePrice, order.baseAmount!! - remainingBaseAmount)

            return if (order.price != null) remainingBaseAmount else 0.0
        }
    }

    override fun remove(orderId: String): Boolean {
        val order = orders[orderId] ?: return false
        val node = levelNodes[order.price]!!

        if (node.data.size == 1) {
            deleteLevel(level = node)
            return true
        }

        node.data.size -= 1
        node.data.baseAmount -= order.quantity
        node.data.quoteAmount -= order.quantity * order.price
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

    private fun deleteLevel(level: RedBlackTree.Node<Level>) {
        val it = level.data.orderIterator()
        while (it.hasNext()) {
            val order = it.next()
            orders.remove(order.id)
        }

        when (level.data.side) {
            Side.BUY -> {
                setToNextBestLevel(level)

                val node = bids.delete(level)
                // This handles the case of when deleting a node with two children the node.data is overridden with a
                // deletionNode.data and then the deletion node is the actual node to be deleted, so the levelNodes
                // map has to be updated.
                if (node.data.price != level.data.price) {
                    levelNodes[node.data.price] = level
                }
            }

            Side.SELL -> {
                setToNextBestLevel(level)

                val node = asks.delete(level)
                // This is again handling of the special case. See above comment on BUY side.
                if (node.data.price != level.data.price) {
                    levelNodes[node.data.price] = level
                }
            }
        }
        levelNodes.remove(level.data.price)
    }

    private fun matchOrdersInLevel(level: Level, baseMatchedAmount: Double) {
        var remainingAmount = baseMatchedAmount
        val limitOrderIt = level.orderIterator()
        while (limitOrderIt.hasNext()) {
            val limitOrder = limitOrderIt.next()

            if (remainingAmount <= 0) break

            val matchedQuantity = minOf(remainingAmount, limitOrder.quantity)
            remainingAmount -= limitOrder.quantity

            if (limitOrder.quantity > matchedQuantity) {
                limitOrder.updateQuantity(limitOrder.quantity - matchedQuantity)
                level.quoteAmount -= limitOrder.baseAmount * limitOrder.price
                level.baseAmount -= limitOrder.baseAmount
            } else {
                remove(limitOrder.id!!)
            }
        }
    }

    private fun setToNextBestLevel(node: RedBlackTree.Node<Level>) {
        when (node.data.side) {
            Side.BUY -> {
                if (node == bestBid) {
                    val it = bids.iteratorFromNode(node, reverse = true)
                    // We call it.next() as the first element returned by the iterator will always be the node that is
                    // passed in.
                    it.next()
                    bestBid = if (it.hasNext()) it.next() else null
                }
            }

            Side.SELL -> {
                if (node == bestAsk) {
                    val it = asks.iteratorFromNode(node)
                    // We call it.next() as the first element returned by the iterator will always be the node that is
                    // passed in.
                    it.next()
                    bestAsk = if (it.hasNext()) it.next() else null
                }
            }
        }
    }

    private fun crossingBidAsk(limitOrder: LimitOrder): Boolean {
        return when (Side.fromString(limitOrder.side)) {
            Side.BUY -> bestAsk != null && (limitOrder.price >= bestAsk!!.data.price)
            Side.SELL -> bestBid != null && (limitOrder.price <= bestBid!!.data.price)
            else -> false
        }
    }
}
package smith.adam.orderbook

import smith.adam.orderbook.model.*
import smith.adam.orderbook.tree.RedBlackTree

class OrderBook(pair: String) : BaseOrderBook(pair) {
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

    override fun add(limitOrder: LimitOrder) {
        if (crossingBidAsk(limitOrder)) {
            limitOrder.quantity = match(limitOrder)
        }

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
                deleteLevel(node)
            }
        }

        weightedAveragePrice /= (totalOrderAmount - remainingQuantity)
        addTrade(order, weightedAveragePrice, totalOrderAmount, remainingQuantity)

        return remainingQuantity
    }

    override fun remove(orderId: String): Boolean {
        val order = orders[orderId] ?: return false
        val node = levelNodes[order.price]!!

        if (node.data.size == 1) {
            deleteLevel(level = node)
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

    private fun setToNextBestLevel(node: RedBlackTree.Node<Level>) {
        when (node.data.side) {
            Side.BUY -> {
                if (node == bestBid) {
                    val it = bids.iteratorFromNode(node)
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
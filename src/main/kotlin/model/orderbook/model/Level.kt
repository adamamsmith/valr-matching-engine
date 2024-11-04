package smith.adam.model.orderbook.model

import smith.adam.model.LimitOrder
import smith.adam.model.orderbook.tree.RedBlackTree.Node
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayDeque

data class Level(
    val side: Side,
    val price: Double,
    var size: Int,
    var totalQuantity: Double,
    var headOrder: BookOrder? = null,
    var tailOrder: BookOrder? = null
) : Comparable<Level> {
    override fun compareTo(other: Level): Int {
        return this.price.compareTo(other.price)
    }

    fun orders(): List<LimitOrder> {
        var order = headOrder
        val result = mutableListOf<LimitOrder>()

        while (order != null) {
            result.add(LimitOrder.fromBookOrder(order))
            order = order.nextOrder
        }
        return result
    }

    fun orderIterator(): Iterator<LimitOrder> {
        return object : Iterator<LimitOrder> {
            private var currentOrder: BookOrder? = null

            override fun hasNext(): Boolean {
                if (currentOrder == null) return headOrder != null

                return currentOrder!!.nextOrder != null
            }

            override fun next(): LimitOrder {
                if (!hasNext()) throw NoSuchElementException()

                currentOrder = if (currentOrder == null) {
                    headOrder
                } else {
                    currentOrder?.nextOrder
                }

                return LimitOrder.fromBookOrder(currentOrder!!)
            }
        }
    }
}
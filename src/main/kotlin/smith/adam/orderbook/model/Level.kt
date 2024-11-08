package smith.adam.orderbook.model

import kotlin.NoSuchElementException

data class Level(
    val side: Side,
    val price: Double,
    var size: Int,
    var totalQuantity: Double,
    var headOrder: LimitOrder? = null,
    var tailOrder: LimitOrder? = null
) : Comparable<Level> {
    override fun compareTo(other: Level): Int {
        return this.price.compareTo(other.price)
    }

    fun orders(): List<LimitOrder> {
        var order = headOrder
        val result = mutableListOf<LimitOrder>()

        while (order != null) {
            result.add(order)
            order = order.nextOrder
        }
        return result
    }

    fun orderIterator(): Iterator<LimitOrder> {
        return object : Iterator<LimitOrder> {
            private var currentOrder: LimitOrder? = null

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

                return currentOrder!!
            }
        }
    }
}
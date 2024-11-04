package smith.adam.model

import kotlinx.serialization.Serializable
import smith.adam.model.orderbook.model.BookOrder
import smith.adam.model.orderbook.model.Level
import smith.adam.model.orderbook.model.Side
import smith.adam.model.orderbook.tree.RedBlackTree

@Serializable
data class LimitOrder(
    val id: String? = null,
    val side: String,
    var quantity: Double,
    val price: Double,
    val pair: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromBookOrder(bookOrder: BookOrder): LimitOrder = LimitOrder(
            id = bookOrder.id,
            side = bookOrder.side,
            quantity = bookOrder.quantity,
            price = bookOrder.price,
            pair = bookOrder.pair,
            timestamp = bookOrder.timestamp,
        )
    }

    fun toLevel(): Level = Level(
        side = Side.fromString(side)!!,
        price = price,
        size = 0,
        totalQuantity = 0.0,
        headOrder = null,
        tailOrder = null
    )

    fun toBookOrder(nextOrder: BookOrder?, previousOrder: BookOrder?, parent: RedBlackTree.Node<Level>?): BookOrder = BookOrder(
        id = id,
        side = side,
        quantity = quantity,
        price = price,
        pair = pair,
        timestamp = timestamp,
        nextOrder = nextOrder,
        previousOrder = previousOrder,
        parentLevel = parent
    )
}


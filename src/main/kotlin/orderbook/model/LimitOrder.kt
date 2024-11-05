package smith.adam.orderbook.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import smith.adam.orderbook.tree.RedBlackTree

@Serializable
data class LimitOrder(
    override val id: String? = null,
    override val side: String,
    var quantity: Double,
    override val price: Double,
    override val pair: String,
    val timestamp: Long = System.currentTimeMillis(),
    @Transient var nextOrder: LimitOrder? = null,
    @Transient var previousOrder: LimitOrder? = null,
    @Transient var parent: RedBlackTree.Node<Level>? = null
) : BaseOrder(id, side, price, quantity, price * quantity, pair) {
    fun toLevel(): Level = Level(
        side = Side.fromString(side)!!,
        price = price,
        size = 0,
        totalQuantity = 0.0,
        headOrder = null,
        tailOrder = null
    )
}


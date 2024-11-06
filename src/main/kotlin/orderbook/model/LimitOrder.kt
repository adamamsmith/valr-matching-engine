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
) : BaseOrder() {
    @Transient override val baseAmount = quantity
    @Transient override val quoteAmount = price * quantity

    fun toLevel(): Level = Level(
        side = Side.fromString(side)!!,
        price = price,
        size = 0,
        totalQuantity = 0.0,
        headOrder = null,
        tailOrder = null
    )

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + side.hashCode()
        result = 31 * result + quantity.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + pair.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return this.hashCode() == other?.hashCode()
    }

    override fun toString(): String {
        return "LimitOrder(id=$id, side='$side', quantity=$quantity, price=$price, pair='$pair', timestamp=$timestamp)"
    }
}


package smith.adam.model.orderbook.model

import smith.adam.model.orderbook.tree.RedBlackTree

data class BookOrder(
    val id: String? = null,
    val side: String,
    val quantity: Double,
    val price: Double,
    val pair: String,
    val timestamp: Long,
    var nextOrder: BookOrder? = null,
    var previousOrder: BookOrder? = null,
    var parentLevel: RedBlackTree.Node<Level>? = null
)
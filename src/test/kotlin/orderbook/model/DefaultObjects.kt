package orderbook.model

import smith.adam.orderbook.model.CancelOrder
import smith.adam.orderbook.model.LimitOrder

fun createDefaultLimitOrder(
    id: String? = null,
    side: String = "BUY",
    quantity: Double = 1.0,
    price: Double = 1.0,
    pair: String = "BTCUSD",
): LimitOrder {
    return LimitOrder(
        id = id,
        side = side,
        quantity = quantity,
        price = price,
        pair = pair
    )
}

fun createDefaultCancelOrder(
    id: String,
    pair: String = "BTCUSD",
): CancelOrder {
    return CancelOrder(
        orderId = id,
        pair = pair
    )
}
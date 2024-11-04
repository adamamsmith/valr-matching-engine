package smith.adam.model.orderbook.model

import smith.adam.model.CancelOrder
import smith.adam.model.LimitOrder
import smith.adam.model.MarketOrder

sealed class OrderEvent {
    data class CreateLimitOrder(val limitOrder: LimitOrder) : OrderEvent()
    data class CreateMarketOrder(val marketOrder: MarketOrder) : OrderEvent()
    data class CancelLimitOrder(val cancelOrder: CancelOrder) : OrderEvent()
}
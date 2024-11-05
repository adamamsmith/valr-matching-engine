package smith.adam.orderbook.model

sealed class OrderEvent {
    data class CreateLimitOrder(val limitOrder: LimitOrder) : OrderEvent()
    data class CreateMarketOrder(val marketOrder: MarketOrder) : OrderEvent()
    data class CancelLimitOrder(val cancelOrder: CancelOrder) : OrderEvent()
}
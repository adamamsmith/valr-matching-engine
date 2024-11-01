package smith.adam.model.orderbook

import smith.adam.model.CancelOrder
import smith.adam.model.LimitOrder
import smith.adam.model.MarketOrder
import smith.adam.model.Trade

class OrderBook : BaseOrderBook() {
    override fun getBook(): Map<String, List<LimitOrder>> {
        TODO("Not yet implemented")
    }

    override fun getTradeHistory(offset: Int, limit: Int): List<Trade> {
        TODO("Not yet implemented")
    }

    override fun add(limitOrder: LimitOrder) {
        TODO("Not yet implemented")
    }

    override fun execute(marketOrder: MarketOrder) {
        TODO("Not yet implemented")
    }

    override fun remove(cancelOrder: CancelOrder) {
        TODO("Not yet implemented")
    }
}
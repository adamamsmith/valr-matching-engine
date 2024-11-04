package smith.adam.service

import smith.adam.model.CancelOrder
import smith.adam.model.LimitOrder
import smith.adam.model.MarketOrder
import smith.adam.model.Trade
import smith.adam.model.orderbook.BaseOrderBook

class OrderService(
    private var orderBooks: MutableMap<String, BaseOrderBook>,
    private val orderValidationService: OrderValidationService
) {
    fun addOrderBook(currencyPair: String, orderBook: BaseOrderBook) {
        orderBooks[currencyPair] = orderBook
    }

    fun placeMarketOrder(marketOrder: MarketOrder): String {
        orderValidationService.validate(marketOrder, orderBooks.keys)
        return orderBooks[marketOrder.pair]!!.placeMarketOrder(marketOrder)
    }

    fun placeLimitOrder(limitOrder: LimitOrder): String {
        orderValidationService.validate(limitOrder, orderBooks.keys)
        return orderBooks[limitOrder.pair]!!.placeLimitOrder(limitOrder)
    }

    fun cancelLimitOrder(cancelOrder: CancelOrder): String {
        orderValidationService.validate(cancelOrder, orderBooks.keys)
        return orderBooks[cancelOrder.pair]!!.cancelLimitOrder(cancelOrder)
    }

    fun getOrderBook(currencyPair: String?): Map<String, List<LimitOrder>> {
        orderValidationService.validate(currencyPair, orderBooks.keys)
        return orderBooks[currencyPair]!!.getBook()
    }

    fun getTradeHistory(currencyPair: String?, offset: Int, limit: Int): List<Trade> {
        orderValidationService.validate(currencyPair, orderBooks.keys)
        return orderBooks[currencyPair]!!.getTradeHistory(offset, limit)
    }
}

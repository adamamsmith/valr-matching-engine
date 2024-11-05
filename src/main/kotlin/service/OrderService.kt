package smith.adam.service

import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import smith.adam.orderbook.model.*
import java.util.*

class OrderService(
    vertx: Vertx,
    private var orderBookIdentifiers: MutableSet<String>,
    private val orderValidationService: OrderValidationService
) {
    private val eventBus: EventBus = vertx.eventBus()

    fun placeMarketOrder(marketOrder: MarketOrder): String {
        orderValidationService.validate(marketOrder, orderBookIdentifiers)
        return eventBus.sendOrder(marketOrder)
    }

    fun placeLimitOrder(limitOrder: LimitOrder): String {
        orderValidationService.validate(limitOrder, orderBookIdentifiers)
        return eventBus.sendOrder(limitOrder)
    }

    fun cancelLimitOrder(cancelOrder: CancelOrder): Promise<Boolean> {
        orderValidationService.validate(cancelOrder, orderBookIdentifiers)
        return eventBus.requestCancelOrder(cancelOrder)
    }

    fun getBook(currencyPair: String?): Promise<Map<String, List<LimitOrder>>> {
        val promise = Promise.promise<Map<String, List<LimitOrder>>>()
        orderValidationService.validate(currencyPair, orderBookIdentifiers)

        eventBus.request<GetBookResponse>(GetBookRequest.address(currencyPair!!), GetBookRequest()) { reply ->
            if (reply.succeeded()) {
                promise.complete(reply.result().body().orderBook)
            } else {
                promise.fail(reply.cause())
            }
        }
        return promise
    }

    fun getTradeHistory(currencyPair: String?, offset: Int, limit: Int): Promise<List<Trade>> {
        val promise = Promise.promise<List<Trade>>()
        orderValidationService.validate(currencyPair, orderBookIdentifiers)

        eventBus.request<GetTradeHistoryResponse>(
            GetTradeHistoryRequest.address(currencyPair!!),
            GetTradeHistoryRequest(offset, limit)
        ) { reply ->
            if (reply.succeeded()) {
                promise.complete(reply.result().body().tradeHistory)
            } else {
                promise.fail(reply.cause())
            }
        }
        return promise
    }

    private fun EventBus.sendOrder(order: BaseOrder): String {
        val orderId = UUID.randomUUID().toString()
        when (order) {
            is MarketOrder -> this.send(
                OrderRequest.address(order.pair),
                OrderRequest.CreateMarketOrder(order.copy(id = orderId))
            )

            is LimitOrder -> this.send(
                OrderRequest.address(order.pair),
                OrderRequest.CreateLimitOrder(order.copy(id = orderId))
            )
        }
        return orderId
    }

    private fun EventBus.requestCancelOrder(order: CancelOrder): Promise<Boolean> {
        val promise = Promise.promise<Boolean>()

        this.request(OrderRequest.address(order.pair), OrderRequest.CancelLimitOrder(order)) { reply ->
            when (reply.succeeded()) {
                true -> promise.complete(reply.result().body())
                false -> promise.fail(reply.cause())
            }
        }
        return promise
    }
}

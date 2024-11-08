package smith.adam.service

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.eventbus.EventBus
import smith.adam.orderbook.model.*

class OrderService(private val orderValidationService: OrderValidationService) {
    private lateinit var eventBus: EventBus

    fun setEventBus(eventBus: EventBus) {
        this.eventBus = eventBus
    }

    fun placeMarketOrder(marketOrder: MarketOrder): Future<String> {
        orderValidationService.validate(marketOrder)
        return eventBus.sendOrder(marketOrder).future()
    }

    fun placeLimitOrder(limitOrder: LimitOrder): Future<String> {
        orderValidationService.validate(limitOrder)
        return eventBus.sendOrder(limitOrder).future()
    }

    fun cancelLimitOrder(cancelOrder: CancelOrder): Future<Boolean> {
        orderValidationService.validate(cancelOrder)
        return eventBus.requestCancelOrder(cancelOrder).future()
    }

    fun getBook(currencyPair: String?): Future<Map<String, List<LimitOrder>>> {
        val promise = Promise.promise<Map<String, List<LimitOrder>>>()
        orderValidationService.validate(currencyPair)
        eventBus.request(GetBookRequest.address(currencyPair!!), GetBookRequest().toJson()) { reply ->
            if (reply.succeeded()) {
                promise.complete(GetBookResponse.fromJson(reply.result().body()).orderBook)
            } else {
                promise.fail(reply.cause())
            }
        }
        return promise.future()
    }

    fun getTradeHistory(currencyPair: String?, offset: Int, limit: Int): Future<List<Trade>> {
        val promise = Promise.promise<List<Trade>>()
        orderValidationService.validate(currencyPair)

        eventBus.request(
            GetTradeHistoryRequest.address(currencyPair!!),
            GetTradeHistoryRequest(offset, limit).toJson()
        ) { reply ->
            if (reply.succeeded()) {
                promise.complete(GetTradeHistoryResponse.fromJson(reply.result().body()).tradeHistory)
            } else {
                promise.fail(reply.cause())
            }
        }
        return promise.future()
    }

    private fun EventBus.sendOrder(order: BaseOrder): Promise<String> {
        val promise = Promise.promise<String>()
        val orderRequest = when (order) {
            is MarketOrder -> OrderRequest.CreateMarketOrder(order)
            is LimitOrder -> OrderRequest.CreateLimitOrder(order)
            else -> throw IllegalArgumentException("Unsupported order type")
        }

        this.request(OrderRequest.address(order.pair), orderRequest.toJson()) { reply ->
            if (reply.succeeded()) {
                promise.complete(reply.result().body())
            } else {
                promise.fail(reply.cause())
            }
        }

        return promise
    }

    private fun EventBus.requestCancelOrder(order: CancelOrder): Promise<Boolean> {
        val promise = Promise.promise<Boolean>()

        this.request(OrderRequest.address(order.pair), OrderRequest.CancelLimitOrder(order).toJson()) { reply ->
            when (reply.succeeded()) {
                true -> promise.complete(reply.result().body())
                false -> promise.fail(reply.cause())
            }
        }
        return promise
    }
}

package smith.adam.service

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.ext.web.handler.HttpException
import kotlinx.serialization.json.Json
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

    fun cancelLimitOrder(cancelOrder: CancelOrder): Future<Boolean> {
        orderValidationService.validate(cancelOrder, orderBookIdentifiers)
        return eventBus.requestCancelOrder(cancelOrder).future()
    }

    fun getBook(currencyPair: String?): Future<Map<String, List<LimitOrder>>> {
        val promise = Promise.promise<Map<String, List<LimitOrder>>>()
        orderValidationService.validate(currencyPair, orderBookIdentifiers)
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
        orderValidationService.validate(currencyPair, orderBookIdentifiers)

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

    private fun EventBus.sendOrder(order: BaseOrder): String {
        val orderId = UUID.randomUUID().toString()
        when (order) {
            is MarketOrder -> this.send(
                OrderRequest.address(order.pair),
                OrderRequest.CreateMarketOrder(order.copy(id = orderId)).toJson()
            )

            is LimitOrder -> this.send(
                OrderRequest.address(order.pair),
                OrderRequest.CreateLimitOrder(order.copy(id = orderId)).toJson()
            )
        }
        return orderId
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

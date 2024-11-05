package smith.adam.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import smith.adam.orderbook.model.CancelOrder
import smith.adam.orderbook.model.LimitOrder
import smith.adam.orderbook.model.MarketOrder
import smith.adam.service.OrderService

fun Application.configureRouting(orderService: OrderService) {
    suspend fun handleRequest(handler: suspend () -> Unit, call: RoutingCall, badRequestMessage: String) {
        try {
            handler()
        } catch (e: BadRequestException) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: badRequestMessage)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred")
        }
    }

    routing {
        post("/orders/market") {
            handleRequest({
                val marketOrderRequest = call.receive<MarketOrder>()
                val orderId = orderService.placeMarketOrder(marketOrderRequest)

                call.respond(HttpStatusCode.Created, orderId)
            }, call, "Invalid market order")
        }

        post("/orders/limit") {
            handleRequest({
                val limitOrderRequest = call.receive<LimitOrder>()
                val orderId = orderService.placeLimitOrder(limitOrderRequest)

                call.respond(HttpStatusCode.Created, orderId)
            }, call, "Invalid limit order")
        }

        delete("/orders/order/{id}") {
            handleRequest({
                val cancelOrderRequest = call.receive<CancelOrder>()
                val orderId = orderService.cancelLimitOrder(cancelOrderRequest)

                call.respond(HttpStatusCode.Created, orderId)
            }, call, "Invalid cancel order")
        }

        get("/public/{currencyPair}/orderbook") {
            handleRequest({
                val currencyPair = call.parameters["currencyPair"]
                val orderBook = orderService.getOrderBook(currencyPair)

                call.respond(HttpStatusCode.OK, orderBook)
            }, call, "Invalid orderbook request")
        }

        get("/marketdata/{currencyPair}/tradehistory") {
            handleRequest({
                val currencyPair = call.parameters["currencyPair"]

                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                val tradeHistory = orderService.getTradeHistory(currencyPair, offset, limit)

                call.respond(HttpStatusCode.OK, tradeHistory)
            }, call, "Invalid trade history request")
        }
    }
}
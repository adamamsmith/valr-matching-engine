package smith.adam.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import smith.adam.model.CancelOrder
import smith.adam.model.LimitOrder
import smith.adam.model.MarketOrder
import smith.adam.service.OrderService

fun Application.configureRouting(orderService: OrderService) {
    routing {
        post("/orders/market") {
            try {
                val marketOrderRequest = call.receive<MarketOrder>()
                val orderId = orderService.placeMarketOrder(marketOrderRequest)

                call.respond(HttpStatusCode.Created, orderId)
            } catch (e: InvalidBodyException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid market order")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred")
            }
        }

        post("/orders/limit") {
            try {
                val limitOrderRequest = call.receive<LimitOrder>()
                val orderId = orderService.placeLimitOrder(limitOrderRequest)

                call.respond(HttpStatusCode.Created, orderId)
            } catch (e: InvalidBodyException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid limit order")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred")
            }
        }

        delete("/orders/order/{id}") {
            try {
                val cancelOrderRequest = call.receive<CancelOrder>()

                if (orderService.cancelLimitOrder(cancelOrderRequest)) {
                    call.respond(HttpStatusCode.OK, cancelOrderRequest.orderId)
                } else call.respond(
                    HttpStatusCode.NotFound, "Order not found for id: ${cancelOrderRequest.orderId}"
                )
            } catch (e: InvalidBodyException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid cancel order")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred")
            }
        }

        get("/public/{currencyPair}/orderbook") {
            try {
                val currencyPair = call.parameters["currencyPair"]
                val orderBook = orderService.getOrderBook(currencyPair)

                call.respond(HttpStatusCode.OK, orderBook)
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid orderbook request")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred")
            }
        }

        get("/marketdata/{currencyPair}/tradehistory") {
            try {
                val currencyPair = call.parameters["currencyPair"]

                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                val tradeHistory = orderService.getTradeHistory(currencyPair, offset, limit)

                call.respond(HttpStatusCode.OK, tradeHistory)
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid trade history request")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred")
            }
        }
    }
}
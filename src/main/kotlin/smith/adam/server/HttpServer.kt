package smith.adam.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.HttpException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import smith.adam.orderbook.model.CancelOrder
import smith.adam.orderbook.model.LimitOrder
import smith.adam.orderbook.model.MarketOrder
import smith.adam.server.model.ErrorResponse
import smith.adam.server.model.OrderResponse
import smith.adam.service.OrderService
import smith.adam.service.OrderValidationService

val logger: Logger = LoggerFactory.getLogger("HttpServer")

class HttpServer(private val port: Int, private val validationService: OrderValidationService) : AbstractVerticle() {
    override fun start() {
        val orderService = OrderService(vertx, validationService)

        val router = configureRouter(vertx, orderService)
        vertx.createHttpServer().requestHandler(router).listen(port) { result ->
            if (result.succeeded()) {
                logger.info("Server is listening on port $port")
            } else {
                logger.error("Failed to start the server: ${result.cause()}")
            }
        }
    }

    private fun configureRouter(vertx: Vertx, orderService: OrderService): Router {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        router.post("/orders/market").handler { ctx ->
            handleRequest(ctx) {
                val marketOrderRequest = Json.decodeFromString<MarketOrder>(ctx.body().asString())
                orderService.placeMarketOrder(marketOrderRequest).onComplete { result ->
                    if (result.succeeded()) {
                        ctx.response().setStatusCode(201)
                            .end(Json.encodeToString(serializer(), OrderResponse(result.result())))
                    } else {
                        buildErrorResponse(ctx, 500)
                    }
                }
            }
        }

        router.post("/orders/limit").handler { ctx ->
            handleRequest(ctx) {
                val limitOrderRequest = Json.decodeFromString<LimitOrder>(ctx.body().asString())
                orderService.placeLimitOrder(limitOrderRequest).onComplete { result ->
                    if (result.succeeded()) {
                        ctx.response().setStatusCode(201)
                            .end(Json.encodeToString(serializer(), OrderResponse(result.result())))
                    } else {
                        buildErrorResponse(ctx, 500)
                    }
                }
            }
        }

        router.delete("/orders/order").handler { ctx ->
            handleRequest(ctx) {
                val cancelOrderRequest = Json.decodeFromString<CancelOrder>(ctx.body().asString())

                orderService.cancelLimitOrder(cancelOrderRequest).onComplete { result ->
                    if (result.succeeded() && result.result()) {
                        ctx.response().setStatusCode(200)
                            .end(Json.encodeToString(serializer(), OrderResponse(cancelOrderRequest.orderId)))
                    } else {
                        buildErrorResponse(ctx, 404, "Order not found for id: ${cancelOrderRequest.orderId}")
                    }
                }
            }
        }

        router.get("/public/:currencyPair/orderbook").handler { ctx ->
            handleRequest(ctx) {
                val currencyPair = ctx.pathParam("currencyPair")

                orderService.getBook(currencyPair).onComplete { result ->
                    if (result.succeeded()) {
                        ctx.response().setStatusCode(200).end(Json.encodeToString(serializer(), result.result()))
                    } else {
                        buildErrorResponse(ctx, 500)
                    }
                }
            }
        }

        router.get("/marketdata/:currencyPair/tradehistory").handler { ctx ->
            handleRequest(ctx) {
                val currencyPair = ctx.pathParam("currencyPair")

                val offset = ctx.queryParam("offset").firstOrNull()?.toIntOrNull() ?: 0
                val limit = ctx.queryParam("limit").firstOrNull()?.toIntOrNull() ?: 100

                orderService.getTradeHistory(currencyPair, offset, limit).onComplete { result ->
                    if (result.succeeded()) {
                        ctx.response().setStatusCode(200).end(Json.encodeToString(serializer(), result.result()))
                    } else {
                        buildErrorResponse(ctx, 500)
                    }
                }
            }
        }

        return router
    }

    private fun handleRequest(ctx: RoutingContext, handler: () -> Unit) {
        try {
            handler()
        } catch (e: HttpException) {
            buildErrorResponse(ctx, e.statusCode, e.payload ?: e.message)
        } catch (e: SerializationException) {
            logger.debug("Malformed request ${ctx.request()}: ", e)
            buildErrorResponse(ctx, 400, "Malformed request")
        } catch (e: Exception) {
            logger.error("Exception handing request ${ctx.request()}: ", e)
            buildErrorResponse(ctx, 500)
        }
    }

    private fun buildErrorResponse(ctx: RoutingContext, status: Int, reason: String? = null) {
        ctx.response().setStatusCode(status).end(
            Json.encodeToString(
                serializer(), ErrorResponse(status, reason ?: "An unexpected error occurred")
            )
        )
    }
}
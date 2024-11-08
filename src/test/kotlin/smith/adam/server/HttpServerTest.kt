package smith.adam.server

import io.mockk.coEvery
import io.mockk.mockk
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.handler.HttpException
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import smith.adam.orderbook.model.CancelOrder
import smith.adam.orderbook.model.LimitOrder
import smith.adam.orderbook.model.MarketOrder
import smith.adam.orderbook.model.Trade
import smith.adam.server.model.ErrorResponse
import smith.adam.server.model.OrderResponse
import smith.adam.service.OrderService
import kotlin.test.assertEquals

@ExtendWith(VertxExtension::class)
class HttpServerTest {

    private lateinit var vertx: Vertx
    private lateinit var mockOrderService: OrderService
    private lateinit var client: WebClient

    @BeforeEach
    fun setup(vertx: Vertx, testContext: VertxTestContext) {
        this.vertx = vertx
        mockOrderService = mockk<OrderService>(relaxed = true)
        client = WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))

        vertx.deployVerticle(HttpServer(8080, mockOrderService)) { result ->
            if (result.succeeded()) testContext.completeNow() else testContext.failNow(result.cause())
        }
    }

    @Test
    fun `placeMarketOrder - success`(testContext: VertxTestContext) {
        val marketOrder = MarketOrder(pair = "BTCUSD", side = "BUY", quoteAmount = 1000.0)
        val expectedOrderId = "12345"

        coEvery { mockOrderService.placeMarketOrder(marketOrder) } returns Future.succeededFuture(expectedOrderId)

        client.post("/orders/market")
            .sendJson(JsonObject(Json.encodeToString(marketOrder))) { ar ->
                if (ar.succeeded()) {
                    testContext.verify {
                        val response = ar.result()
                        assertEquals(201, response.statusCode())
                        assertEquals(expectedOrderId, OrderResponse.fromJson(response.body().toString()).id)
                    }
                    testContext.completeNow()
                } else {
                    testContext.failNow(ar.cause())
                }
            }
    }

    @Test
    fun `placeMarketOrder - 400 error`(testContext: VertxTestContext) {
        val marketOrder = MarketOrder(pair = "BTCUSD", side = "BUY", quoteAmount = 1000.0)

        coEvery { mockOrderService.placeMarketOrder(marketOrder) } throws HttpException(400, "Market order error")

        client.post("/orders/market")
            .sendJson(JsonObject(Json.encodeToString(marketOrder))) { ar ->
                if (ar.succeeded()) {
                    testContext.verify {
                        assertEquals(400, ar.result().statusCode())
                        assertEquals("Market order error", ErrorResponse.fromJson(ar.result().body().toString()).reason)
                    }
                    testContext.completeNow()
                } else {
                    testContext.failNow(ar.cause())
                }
            }
    }

    @Test
    fun `placeLimitOrder - success`(testContext: VertxTestContext) {
        val limitOrder = LimitOrder(pair = "BTCUSD", side = "BUY", price = 1000.0, quantity = 1.0)
        val expectedOrderId = "12345"

        coEvery { mockOrderService.placeLimitOrder(limitOrder) } returns Future.succeededFuture(expectedOrderId)

        client.post("/orders/limit")
            .sendJson(JsonObject(Json.encodeToString(limitOrder))) { ar ->
                if (ar.succeeded()) {
                    testContext.verify {
                        val response = ar.result()
                        assertEquals(201, response.statusCode())
                        assertEquals(expectedOrderId, OrderResponse.fromJson(response.body().toString()).id)
                    }
                    testContext.completeNow()
                } else {
                    testContext.failNow(ar.cause())
                }
            }
    }

    @Test
    fun `placeLimitOrder - 400 error`(testContext: VertxTestContext) {
        val limitOrder = LimitOrder(pair = "BTCUSD", side = "BUY", price = 1000.0, quantity = 1.0)

        coEvery { mockOrderService.placeLimitOrder(limitOrder) } throws HttpException(400, "Limit order error")

        client.post("/orders/limit")
            .sendJson(JsonObject(Json.encodeToString(limitOrder))) { ar ->
                if (ar.succeeded()) {
                    testContext.verify {
                        assertEquals(400, ar.result().statusCode())
                        assertEquals("Limit order error", ErrorResponse.fromJson(ar.result().body().toString()).reason)
                    }
                    testContext.completeNow()
                } else {
                    testContext.failNow(ar.cause())
                }
            }
    }

    @Test
    fun `placeCancelOrder - success`(testContext: VertxTestContext) {
        val cancelOrder = CancelOrder(pair = "BTCUSD", orderId = "12345")

        coEvery { mockOrderService.cancelLimitOrder(cancelOrder) } returns Future.succeededFuture(true)

        client.delete("/orders/order")
            .sendJson(JsonObject(Json.encodeToString(cancelOrder))) { ar ->
                if (ar.succeeded()) {
                    testContext.verify {
                        val response = ar.result()
                        assertEquals(200, response.statusCode())
                        assertEquals(cancelOrder.orderId, OrderResponse.fromJson(response.body().toString()).id)
                    }
                    testContext.completeNow()
                } else {
                    testContext.failNow(ar.cause())
                }
            }
    }

    @Test
    fun `placeCancelOrder - failure`(testContext: VertxTestContext) {
        val cancelOrder = CancelOrder(pair = "BTCUSD", orderId = "12345")

        coEvery { mockOrderService.cancelLimitOrder(cancelOrder) } returns Future.succeededFuture(false)

        client.delete("/orders/order")
            .sendJson(JsonObject(Json.encodeToString(cancelOrder))) { ar ->
                if (ar.succeeded()) {
                    testContext.verify {
                        assertEquals(404, ar.result().statusCode())
                        assertEquals(
                            "Order not found for id: ${cancelOrder.orderId}",
                            ErrorResponse.fromJson(ar.result().body().toString()).reason
                        )
                    }
                    testContext.completeNow()
                } else {
                    testContext.failNow(ar.cause())
                }
            }
    }

    @Test
    fun `cancelLimitOrder - 500 error`(testContext: VertxTestContext) {
        val cancelOrder = CancelOrder(pair = "BTCUSD", orderId = "12345")

        coEvery { mockOrderService.cancelLimitOrder(cancelOrder) } throws Exception("Unexpected error")

        client.delete("/orders/order")
            .sendJson(JsonObject(Json.encodeToString(cancelOrder))) { ar ->
                if (ar.succeeded()) {
                    testContext.verify {
                        assertEquals(500, ar.result().statusCode())
                        assertEquals(
                            "An unexpected error occurred.",
                            ErrorResponse.fromJson(ar.result().body().toString()).reason
                        )
                    }
                    testContext.completeNow()
                } else {
                    testContext.failNow(ar.cause())
                }
            }
    }

    @Test
    fun `getBook - success`(testContext: VertxTestContext) {
        val currencyPair = "BTCUSD"
        val orderBook =
            mapOf(
                "Bids" to listOf(LimitOrder(pair = currencyPair, price = 50000.0, side = "BUY", quantity = 1.0)),
                "Asks" to emptyList()
            )

        coEvery { mockOrderService.getBook(currencyPair) } returns Future.succeededFuture(orderBook)

        client.get("/public/$currencyPair/orderbook").send { ar ->
            if (ar.succeeded()) {
                testContext.verify {
                    assertEquals(200, ar.result().statusCode())
                    val book = Json.decodeFromString<Map<String, List<LimitOrder>>>(ar.result().body().toString())

                    assertEquals(orderBook["Bids"], book["Bids"])
                    assertEquals(orderBook["Asks"], book["Asks"])
                }
                testContext.completeNow()
            } else {
                testContext.failNow(ar.cause())
            }
        }
    }

    @Test
    fun `getBook - error`(testContext: VertxTestContext) {
        val currencyPair = "BTCUSD"

        coEvery { mockOrderService.getBook(currencyPair) } returns Future.failedFuture("Service error")

        client.get("/public/$currencyPair/orderbook").send { ar ->
            if (ar.succeeded()) {
                testContext.verify {
                    assertEquals(500, ar.result().statusCode())
                }
                testContext.completeNow()
            } else {
                testContext.failNow(ar.cause())
            }
        }
    }

    @Test
    fun `getTradeHistory - success`(testContext: VertxTestContext) {
        val currencyPair = "BTCUSD"
        val tradeHistory = listOf(
            Trade(
                id = "123",
                currencyPair = currencyPair,
                price = 50000.0,
                quantity = 0.5,
                tradedAt = 1,
                takerSide = "BUY",
                sequenceId = 0,
                quoteVolume = 0.5
            )
        )

        coEvery { mockOrderService.getTradeHistory(currencyPair, 0, 100) } returns Future.succeededFuture(tradeHistory)

        client.get("/marketdata/$currencyPair/tradehistory").send { ar ->
            if (ar.succeeded()) {
                testContext.verify {
                    assertEquals(200, ar.result().statusCode())
                    assertEquals(
                        tradeHistory,
                        Json.decodeFromString<List<Trade>>(ar.result().body().toString())
                    )
                }
                testContext.completeNow()
            } else {
                testContext.failNow(ar.cause())
            }
        }
    }

    @Test
    fun `getTradeHistory - error`(testContext: VertxTestContext) {
        val currencyPair = "BTCUSD"

        coEvery { mockOrderService.getTradeHistory(currencyPair, 0, 100) } returns Future.failedFuture("Service error")

        client.get("/marketdata/$currencyPair/tradehistory").send { ar ->
            if (ar.succeeded()) {
                testContext.verify {
                    assertEquals(500, ar.result().statusCode())
                }
                testContext.completeNow()
            } else {
                testContext.failNow(ar.cause())
            }
        }
    }
}
package smith.adam.orderbook

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import smith.adam.orderbook.model.*
import java.util.*
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(VertxExtension::class)
class BaseOrderBookTest {

    private lateinit var vertx: Vertx
    private lateinit var orderbook: MockOrderBookImpl
    private lateinit var eventBus: EventBus

    @BeforeEach
    fun setUp(vertx: Vertx) {
        this.vertx = vertx

        orderbook = MockOrderBookImpl("BTCUSD")
        eventBus = vertx.eventBus()
        vertx.deployVerticle(orderbook)
    }

    @Test
    fun `test getBook response`(testContext: VertxTestContext) {
        val message = GetBookRequest().toJson()

        eventBus.request(GetBookRequest.address("BTCUSD"), message) { reply ->
            testContext.verify {
                assertTrue(reply.succeeded())
                val getBookResponse = GetBookResponse.fromJson(reply.result().body())
                assertTrue(getBookResponse.orderBook.isEmpty())
                assertTrue(orderbook.verify_getBook(1))
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `test getTradeHistory response`(testContext: VertxTestContext) {
        val message = GetTradeHistoryRequest(0, 1).toJson()

        eventBus.request(GetTradeHistoryRequest.address("BTCUSD"), message) { reply ->
            testContext.verify {
                assertTrue(reply.succeeded())
                val tradeHistoryResponse = GetTradeHistoryResponse.fromJson(reply.result().body())
                assertEquals(0, tradeHistoryResponse.tradeHistory.size)
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `test handleOrderEvent for limit order creation`(testContext: VertxTestContext) {
        val order = LimitOrder(pair = "BTCUSD", side = "BUY", quantity = 1.0, price = 70000.0)
        val message = OrderRequest.CreateLimitOrder(order).toJson()

        eventBus.request(OrderRequest.address("BTCUSD"), message) { reply ->
            testContext.verify {
                assertTrue(reply.succeeded())
                assertTrue(isUUID(reply.result().body()))
                assertTrue(orderbook.verify_add(1))
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `test handleOrderEvent for market order creation`(testContext: VertxTestContext) {
        val order = MarketOrder(pair = "BTCUSD", side = "SELL", baseAmount = 1.0)
        val message = OrderRequest.CreateMarketOrder(order).toJson()

        eventBus.request(OrderRequest.address("BTCUSD"), message) { reply ->
            testContext.verify {
                assertTrue(reply.succeeded())
                assertTrue(isUUID(reply.result().body()))
                assertTrue(orderbook.verify_match(1))
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `test handleOrderEvent for order cancellation`(testContext: VertxTestContext) {
        val cancelOrder = CancelOrder(orderId = "1", pair = "BTCUSD")
        val message = OrderRequest.CancelLimitOrder(cancelOrder).toJson()

        eventBus.request(OrderRequest.address("BTCUSD"), message) { reply ->
            testContext.verify {
                assertTrue(reply.succeeded())
                assertTrue(reply.result().body())
                assertTrue(orderbook.verify_remove(1))
            }
            testContext.completeNow()
        }
    }

    @Test
    fun `test addTrade`() {
        val baseOrderBookClass = orderbook::class.superclasses.first { it.simpleName == "BaseOrderBook" }

        val addTradeFunction = baseOrderBookClass.declaredMemberFunctions.first { it.name == "addTrade" }
        val getTradeHistory = baseOrderBookClass.declaredMemberFunctions.first { it.name == "getTradeHistory" }
        addTradeFunction.isAccessible = true
        getTradeHistory.isAccessible = true

        val order = MarketOrder(id = "1", pair = "BTCUSD", side = "BUY", quoteAmount = 50000.0)
        val weightedAveragePrice = 50000.0
        val quantity = 1.0
        addTradeFunction.call(orderbook, order, weightedAveragePrice, quantity, 0.0)

        val trade: Trade = (getTradeHistory.call(orderbook, 0, 10) as List<Trade>).first()

        assertEquals(order.id, trade.id)
        assertEquals("BTCUSD", trade.currencyPair)
        assertEquals(weightedAveragePrice, trade.price)
        assertEquals(quantity, trade.quantity)
        assertEquals(trade.sequenceId, 0)
    }

    // Helper Functions for testing
    private fun isUUID(input: String): Boolean {
        return try {
            UUID.fromString(input)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    // Mock implementation, Note: Didn't use a mocking lib as I wasn't sure how to get it to play nice with Vertx.
    class MockOrderBookImpl(pair: String) : BaseOrderBook(pair) {
        private var getBook = 0
        private var add = 0
        private var match = 0
        private var remove = 0

        override fun getBook(): Map<String, List<LimitOrder>> {
            getBook += 1
            return emptyMap()
        }

        override fun add(limitOrder: LimitOrder) {
            add += 1
        }

        override fun match(order: BaseOrder): Double {
            match += 1
            return 0.0
        }

        override fun remove(orderId: String): Boolean {
            remove += 1
            return true
        }

        fun verify_getBook(numberTimes: Int = 0): Boolean {
            return getBook == numberTimes
        }

        fun verify_add(numberTimes: Int = 0): Boolean {
            return add == numberTimes
        }

        fun verify_match(numberTimes: Int = 0): Boolean {
            return match == numberTimes
        }

        fun verify_remove(numberTimes: Int = 0): Boolean {
            return remove == numberTimes
        }
    }
}

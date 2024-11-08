package smith.adam.service

import io.mockk.*
import io.vertx.core.*
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.junit5.VertxExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import smith.adam.orderbook.model.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(VertxExtension::class)
class OrderServiceTest {

    private lateinit var vertx: Vertx
    private lateinit var orderService: OrderService
    private lateinit var mockOrderValidationService: OrderValidationService

    @BeforeEach
    fun setUp() {
        vertx = Vertx.vertx()
        mockOrderValidationService = mockk()

        orderService = OrderService(vertx, mockOrderValidationService)

        every { mockOrderValidationService.validate(any<MarketOrder>()) } returns Unit
        every { mockOrderValidationService.validate(any<LimitOrder>()) } returns Unit
        every { mockOrderValidationService.validate(any<CancelOrder>()) } returns Unit
        every { mockOrderValidationService.validate(any<String>()) } returns Unit
    }

    @Test
    fun `placeMarketOrder should send market order and return order id`() {
        val expectedOrderId = "orderId"
        val eventListener = EventListener(OrderRequest.address("BTCUSD"), expectedOrderId)
        vertx.deployVerticle(eventListener)

        val marketOrder = MarketOrder(pair = "BTCUSD", side = "BUY", quoteAmount = 1000.0)

        val result = orderService.placeMarketOrder(marketOrder)

        // We need to sleep otherwise the eventListener never has time to reply
        Thread.sleep(10)

        assertEquals(expectedOrderId, result.result())
        assertTrue(eventListener.messageReceived(OrderRequest.CreateMarketOrder(marketOrder).toJson()))
    }

    @Test
    fun `placeLimitOrder should send limit order and return order id`() {
        val expectedOrderId = "orderId"
        val eventListener = EventListener(OrderRequest.address("BTCUSD"), expectedOrderId)
        vertx.deployVerticle(eventListener)

        val limitOrder = LimitOrder(pair = "BTCUSD", side = "BUY", price = 1000.0, quantity = 1.0)

        val result = orderService.placeLimitOrder(limitOrder)

        // We need to sleep otherwise the eventListener never has time to reply
        Thread.sleep(10)

        assertEquals(expectedOrderId, result.result())
        assertTrue(eventListener.messageReceived(OrderRequest.CreateLimitOrder(limitOrder).toJson()))
    }

    @Test
    fun `placeCancelOrder should send limit order and return Boolean`() {
        val eventListener = EventListener(OrderRequest.address("BTCUSD"), true)
        vertx.deployVerticle(eventListener)

        val cancelOrder = CancelOrder(pair = "BTCUSD", orderId = "12345")

        val result = orderService.cancelLimitOrder(cancelOrder)

        // We need to sleep otherwise the eventListener never has time to reply
        Thread.sleep(10)

        assertTrue(result.result())
        assertTrue(eventListener.messageReceived(OrderRequest.CancelLimitOrder(cancelOrder).toJson()))
    }

    @Test
    fun `placeCancelOrder should send limit order and return order id`() {
        val eventListener = EventListener(OrderRequest.address("BTCUSD"), true)
        vertx.deployVerticle(eventListener)

        val cancelOrder = CancelOrder(pair = "BTCUSD", orderId = "12345")

        val result = orderService.cancelLimitOrder(cancelOrder)

        // We need to sleep otherwise the eventListener never has time to reply
        Thread.sleep(10)

        assertTrue(result.result())
        assertTrue(eventListener.messageReceived(OrderRequest.CancelLimitOrder(cancelOrder).toJson()))
    }

    @Test
    fun `getBook should send and return Map`() {
        val eventListener = EventListener(GetBookRequest.address("BTCUSD"), "{ \"orderBook\": {}}")
        vertx.deployVerticle(eventListener)

        val result = orderService.getBook("BTCUSD")

        // We need to sleep otherwise the eventListener never has time to reply
        Thread.sleep(10)

        assertEquals(result.result(), emptyMap())
        assertTrue(eventListener.messageReceived("{}"))
    }

    @Test
    fun `getTradeHistory should send and return List`() {
        val eventListener = EventListener(GetTradeHistoryRequest.address("BTCUSD"), "{ \"tradeHistory\": []}")
        vertx.deployVerticle(eventListener)

        val result = orderService.getTradeHistory("BTCUSD", 0, 10)

        // We need to sleep otherwise the eventListener never has time to reply
        Thread.sleep(10)

        assertEquals(result.result(), emptyList())
        assertTrue(eventListener.messageReceived("{\"offset\":0,\"limit\":10}"))
    }

    // Test Helpers
    class EventListener(private val path: String, private val reply: Any? = null) : AbstractVerticle() {
        private var messageReceived: Any? = null

        override fun start() {
            val eventBus: EventBus = vertx.eventBus()
            eventBus.consumer(path) { message ->
                handleEvent(message)
                if (reply != null) {
                    message.reply(reply)
                }
            }
        }

        private fun handleEvent(message: Message<String>) {
            messageReceived = message.body()
        }

        fun messageReceived(message: Any): Boolean {
            return message == messageReceived
        }
    }
}
package smith.adam.service

import io.vertx.ext.web.handler.HttpException
import smith.adam.orderbook.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class OrderValidationServiceTest {

    private lateinit var orderValidationService: OrderValidationService
    private val validCurrencyPairs = mutableSetOf("BTCUSD", "ETHUSD")

    @BeforeEach
    fun setUp() {
        orderValidationService = OrderValidationService(validCurrencyPairs)
    }

    @Test
    fun `validate market order with valid data should pass`() {
        val marketOrder = MarketOrder(id = "1", pair = "BTCUSD", side = "BUY", quoteAmount = 1000.0)
        orderValidationService.validate(marketOrder)
    }

    @Test
    fun `validate market order with invalid currency pair should throw exception`() {
        val marketOrder = MarketOrder(id = "1", pair = "INVALID", side = "BUY", quoteAmount = 1000.0, baseAmount = 1.0)

        val exception = assertThrows<HttpException> {
            orderValidationService.validate(marketOrder)
        }
        assertEquals("Market does not exist for pair INVALID.", exception.payload)
    }

    @Test
    fun `validate market order with invalid amount should throw exception`() {
        val marketOrder = MarketOrder(id = "1", pair = "BTCUSD", side = "BUY", quoteAmount = -1.0)

        val exception = assertThrows<HttpException> {
            orderValidationService.validate(marketOrder)
        }
        assertEquals("quoteAmount must be non-negative when side is BUY.", exception.payload)
    }

    @Test
    fun `validate market order with wrong amount specified should throw exception`() {
        val marketOrder = MarketOrder(id = "1", pair = "BTCUSD", side = "BUY", baseAmount = 1.0)

        val exception = assertThrows<HttpException> {
            orderValidationService.validate(marketOrder)
        }
        assertEquals("quoteAmount must be non-negative when side is BUY.", exception.payload)
    }

    @Test
    fun `validate market order with multiple things wrong should throw exception`() {
        val marketOrder = MarketOrder(id = "1", pair = "INVALID", side = "BUY", baseAmount = 1.0)

        val exception = assertThrows<HttpException> {
            orderValidationService.validate(marketOrder)
        }
        assertEquals(
            "Market does not exist for pair INVALID. quoteAmount must be non-negative when side is BUY.",
            exception.payload
        )
    }


    @Test
    fun `validate limit order with valid data should pass`() {
        val limitOrder = LimitOrder(id = "1", pair = "BTCUSD", side = "BUY", price = 50000.0, quantity = 1.0)
        orderValidationService.validate(limitOrder)
    }

    @Test
    fun `validate limit order with invalid currency pair should throw exception`() {
        val limitOrder = LimitOrder(id = "1", pair = "INVALID", side = "BUY", price = 50000.0, quantity = 1.0)

        val exception = assertThrows<HttpException> {
            orderValidationService.validate(limitOrder)
        }
        assertEquals("Market does not exist for pair INVALID.", exception.payload)
    }

    @Test
    fun `validate limit order with invalid amounts should throw exception`() {
        val limitOrder = LimitOrder(id = "1", pair = "BTCUSD", side = "BUY", price = -50000.0, quantity = -1.0)

        val exception = assertThrows<HttpException> {
            orderValidationService.validate(limitOrder)
        }
        assertEquals("price must be non-negative. quantity must be non-negative.", exception.payload)
    }

    @Test
    fun `validate limit order with invalid side should throw exception`() {
        val limitOrder = LimitOrder(id = "1", pair = "BTCUSD", side = "INVALID", price = 50000.0, quantity = 1.0)

        val exception = assertThrows<HttpException> {
            orderValidationService.validate(limitOrder)
        }
        assertEquals("Invalid side: INVALID.", exception.payload)
    }

    @Test
    fun `validate cancel order with valid data should pass`() {
        val cancelOrder = CancelOrder(pair = "BTCUSD", orderId = "12345")
        orderValidationService.validate(cancelOrder)
    }

    @Test
    fun `validate cancel order with invalid currency pair should throw exception`() {
        val cancelOrder = CancelOrder(pair = "INVALID", orderId = "12345")

        val exception = assertThrows<HttpException> {
            orderValidationService.validate(cancelOrder)
        }
        assertEquals("Market does not exist for pair INVALID.", exception.payload)
    }

    @Test
    fun `validate null currency pair should throw exception`() {
        val exception = assertThrows<HttpException> {
            orderValidationService.validate(null)
        }
        assertEquals("Market must be provided.", exception.payload)
    }

    @Test
    fun `validate valid currency pair should pass`() {
        orderValidationService.validate("ETHUSD")
    }

    @Test
    fun `validate invalid currency pair should throw exception`() {
        val exception = assertThrows<HttpException> {
            orderValidationService.validate("INVALID")
        }
        assertEquals("Market does not exist for pair INVALID.", exception.payload)
    }
}
package orderbook

import orderbook.model.createDefaultLimitOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import smith.adam.orderbook.OrderBook

class OrderBookTest {
    private val simpleOrderBook = smith.adam.orderbook.SimpleOrderBook("BTCUSD")
    private val orderBook = OrderBook("BTCUSD")

    private val defaultBids =
        (1..20).associateWith { i -> createDefaultLimitOrder(id = "$i", price = i.toDouble()) }.toMutableMap()

    private val defaultAsks =
        (10..30).associateWith { i -> createDefaultLimitOrder(id = "$i", price = i.toDouble(), side = "SELL") }
            .toMutableMap()

    private val ordersToPlace = listOf(3, -20, -17, 6, -13, 1, 8, 7, -22, 12, -14)

    @BeforeEach
    fun setUp() {
//        ordersToPlace.map { i -> simpleOrderBook.placeLimitOrder(if (i < 0) defaultAsks[abs(i)]!! else defaultBids[i]!!) }
//        ordersToPlace.map { i -> orderBook.placeLimitOrder(if (i < 0) defaultAsks[abs(i)]!! else defaultBids[i]!!) }

        // TODO: There must be a better way to do this
        Thread.sleep(10)
    }

    @Test
    fun testGetBook() {
        val expected = simpleOrderBook.getBook()
        val actual = orderBook.getBook()

        println(expected)
        println()
        println(actual)

        assertEquals(expected, actual)
    }

    @Test
    fun testGetTradeHistory() {
    }

    @Test
    fun testAdd() {
    }

    @Test
    fun testExecute() {
    }

    @Test
    fun testRemove() {
    }
}
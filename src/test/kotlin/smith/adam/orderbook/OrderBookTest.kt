package smith.adam.orderbook

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import smith.adam.orderbook.model.Level
import smith.adam.orderbook.model.LimitOrder
import smith.adam.orderbook.model.MarketOrder
import smith.adam.orderbook.model.Trade
import smith.adam.orderbook.tree.RedBlackTree
import kotlin.random.Random
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderBookTest {

    private lateinit var orderBook: OrderBook

    @BeforeEach
    fun setup() {
        orderBook = OrderBook("BTCUSD")
    }

    @Test
    fun `add - bid`() {
        val buyOrder = LimitOrder("12345", "BUY", 1.0, 50000.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")

        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        val levelNodes = getPrivateProperty<MutableMap<Double, RedBlackTree.Node<Level>>>(orderBook, "levelNodes")
        val orders = getPrivateProperty<MutableMap<String, LimitOrder>>(orderBook, "orders")

        assertEquals(buyOrder, bestBid?.data?.headOrder)
        assertNull(bestAsk)
        assertEquals(listOf(buyOrder), bids)
        assertEquals(emptyList(), asks)

        assertTrue(buyOrder.id in orders)
        assertEquals(buyOrder, orders[buyOrder.id])

        assertTrue(buyOrder.price in levelNodes)
        assertEquals(buyOrder, levelNodes[buyOrder.price]?.data?.headOrder)
    }

    @Test
    fun `add - bid at same level`() {
        val buyOrder = LimitOrder("12345", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("12346", "BUY", 1.0, 50000.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        assertEquals(buyOrder, bestBid?.data?.headOrder)
        assertNull(bestAsk)
        assertEquals(listOf(buyOrder, buyOrder2), bids)
        assertEquals(emptyList(), asks)

        assertEquals(buyOrder.quantity + buyOrder2.quantity, bestBid?.data?.baseAmount)
        assertEquals(
            buyOrder.quantity * buyOrder.price + buyOrder2.quantity * buyOrder2.price,
            bestBid?.data?.quoteAmount
        )
    }

    @Test
    fun `add - bid at different level`() {
        val buyOrder = LimitOrder("12345", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("12346", "BUY", 1.0, 49999.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        assertEquals(buyOrder, bestBid?.data?.headOrder)
        assertNull(bestAsk)
        assertEquals(listOf(buyOrder, buyOrder2), bids)
        assertEquals(emptyList(), asks)
    }

    @Test
    fun `add - ask`() {
        val sellOrder = LimitOrder("12345", "SELL", 1.0, 50000.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        val levelNodes = getPrivateProperty<MutableMap<Double, RedBlackTree.Node<Level>>>(orderBook, "levelNodes")
        val orders = getPrivateProperty<MutableMap<String, LimitOrder>>(orderBook, "orders")

        assertEquals(sellOrder, bestAsk?.data?.headOrder)
        assertNull(bestBid)
        assertEquals(listOf(sellOrder), asks)
        assertEquals(emptyList(), bids)

        assertTrue(sellOrder.id in orders)
        assertEquals(sellOrder, orders[sellOrder.id])

        assertTrue(sellOrder.price in levelNodes)
        assertEquals(sellOrder, levelNodes[sellOrder.price]?.data?.headOrder)
    }

    @Test
    fun `add - ask at same level`() {
        val sellOrder = LimitOrder("12345", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("12346", "SELL", 1.0, 50000.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)
        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        assertEquals(sellOrder, bestAsk?.data?.headOrder)
        assertNull(bestBid)
        assertEquals(listOf(sellOrder, sellOrder2), asks)
        assertEquals(emptyList(), bids)

        assertEquals(sellOrder.quantity + sellOrder2.quantity, bestAsk?.data?.baseAmount)
        assertEquals(
            sellOrder.quantity * sellOrder.price + sellOrder.quantity * sellOrder.price,
            bestAsk?.data?.quoteAmount
        )
    }

    @Test
    fun `add - ask at different level`() {
        val sellOrder = LimitOrder("12345", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("12346", "SELL", 1.0, 50001.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)
        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        assertEquals(sellOrder, bestAsk?.data?.headOrder)
        assertNull(bestBid)
        assertEquals(listOf(sellOrder, sellOrder2), asks)
        assertEquals(emptyList(), bids)
    }

    @Test
    fun `add - multiple bids and asks`() {

        val buyOrders =
            (50000 downTo 49900).associateWith { i -> createDefaultLimitOrder(id = "$i", price = i.toDouble()) }
                .toMutableMap()
        val sellOrders = (50001..50100).associateWith { i ->
            createDefaultLimitOrder(
                id = "$i",
                price = i.toDouble(),
                side = "SELL"
            )
        }.toMutableMap()

        val range = 49980..50021
        val orderIndexes = List(50) { Random.nextInt(range.first, range.last + 1) }

        orderIndexes.map { i ->
            callPrivateMethod<Unit>(
                orderBook,
                "add",
                if (i > 50000) sellOrders[i] else buyOrders[i]
            )
        }

        val bidIndexes = orderIndexes.filter { it < 50001 }
        val askIndexes = orderIndexes.filter { it > 50000 }

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")

        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        val expectedBestBidIndex = bidIndexes.maxOrNull()
        val expectedBestBid = if (expectedBestBidIndex != null) buyOrders[expectedBestBidIndex] else null
        val expectedBestAskIndex = askIndexes.minOrNull()
        val expectedBestAsk = if (expectedBestAskIndex != null) sellOrders[expectedBestAskIndex] else null

        assertEquals(expectedBestBid, bestBid?.data?.headOrder)
        assertEquals(expectedBestAsk, bestAsk?.data?.headOrder)

        assertEquals(bidIndexes.sortedDescending().map { buyOrders[it]!! }, bids)
        assertEquals(askIndexes.sorted().map { sellOrders[it]!! }, asks)
    }

    @Test
    fun `add - limit BUY crossing bid ask spread full`() {
        val sellOrder = LimitOrder("54321", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("54322", "SELL", 1.0, 50001.0, pair = "BTCUSD")
        val sellOrder3 = LimitOrder("54323", "SELL", 1.0, 50002.0, pair = "BTCUSD")
        val buyOrder = LimitOrder("1", "BUY", price = 50005.0, quantity = 1.5, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder3)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder)

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        sellOrder2.updateQuantity(0.5)

        assertEquals(sellOrder2, bestAsk?.data?.headOrder)
        assertNull(bestBid)
        assertEquals(listOf(sellOrder2, sellOrder3), asks)
        assertEquals(emptyList(), bids)
        assertEquals(sellOrder2.quantity, bestAsk?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(50000.333333333336, tradeHistory.first().price)
        assertEquals(1.5, tradeHistory.first().quantity)
        assertEquals(sellOrder.price + sellOrder2.price * 0.5, tradeHistory.first().quoteVolume)
        assertEquals(sellOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(buyOrder.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `add - limit BUY crossing bid ask spread partial`() {
        val sellOrder = LimitOrder("54321", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("54322", "SELL", 1.0, 50001.0, pair = "BTCUSD")
        val sellOrder3 = LimitOrder("54323", "SELL", 1.0, 50002.0, pair = "BTCUSD")
        val buyOrder = LimitOrder("0", "BUY", price = 49999.0, quantity = 1.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("1", "BUY", price = 50001.0, quantity = 2.5, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder3)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        buyOrder2.updateQuantity(0.5)

        assertEquals(sellOrder3, bestAsk?.data?.headOrder)
        assertEquals(buyOrder2, bestBid?.data?.headOrder)
        assertEquals(listOf(sellOrder3), asks)
        assertEquals(listOf(buyOrder2, buyOrder), bids)
        assertEquals(sellOrder3.quantity, bestAsk?.data?.baseAmount)
        assertEquals(buyOrder2.quantity, bestBid?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(50000.5, tradeHistory.first().price)
        assertEquals(sellOrder.price + sellOrder2.price, tradeHistory.first().quoteVolume)
        assertEquals(sellOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(buyOrder2.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `add - limit SELL crossing bid ask spread full`() {
        val buyOrder = LimitOrder("54321", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("54322", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder3 = LimitOrder("54323", "BUY", 1.0, 49999.0, pair = "BTCUSD")
        val sellOrder = LimitOrder("1", "SELL", price = 0.0, quantity = 1.5, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder3)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder)

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        buyOrder2.updateQuantity(0.5)

        assertEquals(buyOrder2, bestBid?.data?.headOrder)
        assertNull(bestAsk)
        assertEquals(listOf(buyOrder2, buyOrder3), bids)
        assertEquals(emptyList(), asks)
        assertEquals(buyOrder2.quantity, bestBid?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(buyOrder.price, tradeHistory.first().price)
        assertEquals(sellOrder.baseAmount, tradeHistory.first().quantity)
        assertEquals(buyOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(sellOrder.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `add - limit SELL crossing bid ask spread partial`() {
        val buyOrder = LimitOrder("54321", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("54322", "BUY", 1.0, 49999.0, pair = "BTCUSD")
        val buyOrder3 = LimitOrder("54323", "BUY", 1.0, 49998.0, pair = "BTCUSD")
        val sellOrder = LimitOrder("1", "SELL", price = 50001.0, quantity = 1.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("1", "SELL", price = 49999.0, quantity = 2.5, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder3)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        sellOrder2.updateQuantity(0.5)

        assertEquals(buyOrder3, bestBid?.data?.headOrder)
        assertEquals(sellOrder2, bestAsk?.data?.headOrder)
        assertEquals(listOf(buyOrder3), bids)
        assertEquals(listOf(sellOrder2, sellOrder), asks)
        assertEquals(buyOrder3.quantity, bestBid?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(49999.5, tradeHistory.first().price)
        assertEquals(2.0, tradeHistory.first().quantity)
        assertEquals(buyOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(sellOrder2.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `remove - bid`() {
        val buyOrder = LimitOrder("12345", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        callPrivateMethod<Unit>(orderBook, "add", buyOrder)

        assertTrue(callPrivateMethod(orderBook, "remove", buyOrder.id!!))

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList { it.orders() }

        val levelNodes = getPrivateProperty<MutableMap<Double, RedBlackTree.Node<Level>>>(orderBook, "levelNodes")
        val orders = getPrivateProperty<MutableMap<String, LimitOrder>>(orderBook, "orders")

        assertNull(bestBid)
        assertEquals(emptyList(), bids)

        assertTrue(buyOrder.id !in orders)
        assertTrue(buyOrder.price !in levelNodes)
    }

    @Test
    fun `remove - bid other levels`() {
        val buyOrder = LimitOrder("12345", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("12346", "BUY", 1.0, 49999.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)

        assertTrue(callPrivateMethod(orderBook, "remove", buyOrder.id!!))

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList { it.orders() }

        assertEquals(buyOrder2, bestBid?.data?.headOrder)
        assertEquals(listOf(buyOrder2), bids)
    }

    @Test
    fun `remove - bid but not level`() {
        val buyOrder = LimitOrder("12345", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("12346", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder3 = LimitOrder("12347", "BUY", 1.0, 49999.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder3)

        assertTrue(callPrivateMethod(orderBook, "remove", buyOrder.id!!))

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }

        assertEquals(buyOrder2, bestBid?.data?.headOrder)
        assertEquals(listOf(buyOrder2, buyOrder3), bids)

        assertEquals(buyOrder2.quantity, bestBid?.data?.baseAmount)
        assertEquals(buyOrder2.quantity * buyOrder2.price, bestBid?.data?.quoteAmount)
    }

    @Test
    fun `remove - ask`() {
        val sellOrder = LimitOrder("54321", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        callPrivateMethod<Unit>(orderBook, "add", sellOrder)

        assertTrue(callPrivateMethod(orderBook, "remove", sellOrder.id!!))

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        val levelNodes = getPrivateProperty<MutableMap<Double, RedBlackTree.Node<Level>>>(orderBook, "levelNodes")
        val orders = getPrivateProperty<MutableMap<String, LimitOrder>>(orderBook, "orders")

        assertNull(bestAsk)
        assertEquals(emptyList(), asks)
        assertTrue(sellOrder.id !in orders)
        assertTrue(sellOrder.price !in levelNodes)
    }

    @Test
    fun `remove - ask other levels`() {
        val sellOrder = LimitOrder("54321", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("54322", "SELL", 1.0, 50001.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)

        assertTrue(callPrivateMethod(orderBook, "remove", sellOrder.id!!))

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        assertEquals(sellOrder2, bestAsk?.data?.headOrder)
        assertEquals(listOf(sellOrder2), asks)
    }

    @Test
    fun `remove - ask but not level`() {
        val sellOrder = LimitOrder("54321", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("54322", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder3 = LimitOrder("54323", "SELL", 1.0, 50001.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder3)

        assertTrue(callPrivateMethod(orderBook, "remove", sellOrder.id!!))

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }

        assertEquals(sellOrder2, bestAsk?.data?.headOrder)
        assertEquals(listOf(sellOrder2, sellOrder3), asks)
        assertEquals(sellOrder2.quantity, bestAsk?.data?.baseAmount)
        assertEquals(sellOrder2.quantity * sellOrder2.price, bestAsk?.data?.quoteAmount)
    }

    @Test
    fun `match - market BUY full match`() {
        val sellOrder = LimitOrder("54321", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("54322", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder3 = LimitOrder("54323", "SELL", 1.0, 50001.0, pair = "BTCUSD")
        val buyOrder = MarketOrder("1", "BUY", quoteAmount = 50000.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder3)
        assertEquals(0.0, callPrivateMethod<Double>(orderBook, "match", buyOrder))

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        assertEquals(sellOrder2, bestAsk?.data?.headOrder)
        assertEquals(listOf(sellOrder2, sellOrder3), asks)
        assertEquals(sellOrder2.quantity, bestAsk?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(sellOrder.price, tradeHistory.first().price)
        assertEquals(buyOrder.quoteAmount, tradeHistory.first().quoteVolume)
        assertEquals(sellOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(buyOrder.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `match - market SELL full match`() {
        val buyOrder = LimitOrder("54321", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("54322", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder3 = LimitOrder("54323", "BUY", 1.0, 49999.0, pair = "BTCUSD")
        val sellOrder = MarketOrder("1", "SELL", baseAmount = 1.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder3)
        assertEquals(0.0, callPrivateMethod<Double>(orderBook, "match", sellOrder))

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        assertEquals(buyOrder2, bestBid?.data?.headOrder)
        assertEquals(listOf(buyOrder2, buyOrder3), bids)
        assertEquals(buyOrder2.quantity, bestBid?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(buyOrder.price, tradeHistory.first().price)
        assertEquals(sellOrder.baseAmount, tradeHistory.first().quantity)
        assertEquals(buyOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(sellOrder.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `match - market BUY partial match`() {
        val sellOrder = LimitOrder("54321", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("54322", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder3 = LimitOrder("54323", "SELL", 1.0, 50001.0, pair = "BTCUSD")
        val buyOrder = MarketOrder("1", "BUY", quoteAmount = 25000.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder3)
        assertEquals(0.0, callPrivateMethod<Double>(orderBook, "match", buyOrder))

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        sellOrder.updateQuantity(sellOrder.quantity - buyOrder.quoteAmount!! / sellOrder.price)

        assertEquals(sellOrder, bestAsk?.data?.headOrder)
        assertEquals(listOf(sellOrder, sellOrder2, sellOrder3), asks)
        assertEquals(sellOrder.quantity + sellOrder2.quantity, bestAsk?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(sellOrder.price, tradeHistory.first().price)
        assertEquals(buyOrder.quoteAmount, tradeHistory.first().quoteVolume)
        assertEquals(sellOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(buyOrder.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `match - market SELL partial match`() {
        val buyOrder = LimitOrder("54321", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("54322", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder3 = LimitOrder("54323", "BUY", 1.0, 49999.0, pair = "BTCUSD")
        val sellOrder = MarketOrder("1", "SELL", baseAmount = 0.5, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder3)
        assertEquals(0.0, callPrivateMethod<Double>(orderBook, "match", sellOrder))

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        buyOrder.updateQuantity(buyOrder.quantity - sellOrder.baseAmount!!)

        assertEquals(buyOrder, bestBid?.data?.headOrder)
        assertEquals(listOf(buyOrder, buyOrder2, buyOrder3), bids)
        assertEquals(buyOrder.quantity + buyOrder2.quantity, bestBid?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(buyOrder.price, tradeHistory.first().price)
        assertEquals(sellOrder.baseAmount, tradeHistory.first().quantity)
        assertEquals(buyOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(sellOrder.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `match - market BUY multiple levels`() {
        val sellOrder = LimitOrder("54321", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("54322", "SELL", 1.0, 50001.0, pair = "BTCUSD")
        val sellOrder3 = LimitOrder("54323", "SELL", 1.0, 50002.0, pair = "BTCUSD")
        val buyOrder = MarketOrder("1", "BUY", quoteAmount = 125002.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder3)
        assertEquals(0.0, callPrivateMethod<Double>(orderBook, "match", buyOrder))

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        sellOrder3.updateQuantity(0.5)

        assertEquals(sellOrder3, bestAsk?.data?.headOrder)
        assertEquals(listOf(sellOrder3), asks)
        assertEquals(sellOrder3.quantity, bestAsk?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(50000.8, tradeHistory.first().price)
        assertEquals(buyOrder.quoteAmount, tradeHistory.first().quoteVolume)
        assertEquals(2.5, tradeHistory.first().quantity)
        assertEquals(sellOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(buyOrder.side, tradeHistory.first().takerSide)

    }

    @Test
    fun `match - market SELL multiple levels`() {
        val buyOrder = LimitOrder("54321", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("54322", "BUY", 1.0, 49999.0, pair = "BTCUSD")
        val buyOrder3 = LimitOrder("54323", "BUY", 1.0, 49998.0, pair = "BTCUSD")
        val sellOrder = MarketOrder("1", "SELL", baseAmount = 2.5, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder3)
        assertEquals(0.0, callPrivateMethod<Double>(orderBook, "match", sellOrder))

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        buyOrder3.updateQuantity(0.5)

        assertEquals(buyOrder3, bestBid?.data?.headOrder)
        assertEquals(listOf(buyOrder3), bids)
        assertEquals(buyOrder3.quantity, bestBid?.data?.baseAmount)

        assertEquals(1, tradeHistory.size)
        assertEquals(49999.2, tradeHistory.first().price)
        assertEquals(sellOrder.baseAmount, tradeHistory.first().quantity)
        assertEquals(124998.0, tradeHistory.first().quoteVolume)
        assertEquals(buyOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(sellOrder.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `match - market BUY more than book`() {
        val sellOrder = LimitOrder("54321", "SELL", 1.0, 50000.0, pair = "BTCUSD")
        val sellOrder2 = LimitOrder("54322", "SELL", 1.0, 50001.0, pair = "BTCUSD")
        val sellOrder3 = LimitOrder("54323", "SELL", 1.0, 50002.0, pair = "BTCUSD")
        val buyOrder = MarketOrder("1", "BUY", quoteAmount = 150004.0, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", sellOrder)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder2)
        callPrivateMethod<Unit>(orderBook, "add", sellOrder3)
        assertEquals(0.0, callPrivateMethod<Double>(orderBook, "match", buyOrder))

        val bestAsk = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestAsk")
        val asks = getPrivateProperty<RedBlackTree<Level>>(orderBook, "asks").toList { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        assertNull(bestAsk)
        assertEquals(emptyList(), asks)

        assertEquals(1, tradeHistory.size)
        assertEquals(50001.0, tradeHistory.first().price)
        assertEquals(buyOrder.quoteAmount!! - 1.0, tradeHistory.first().quoteVolume)
        assertEquals(3.0, tradeHistory.first().quantity)
        assertEquals(sellOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(buyOrder.side, tradeHistory.first().takerSide)
    }

    @Test
    fun `match - market SELL more than book`() {
        val buyOrder = LimitOrder("54321", "BUY", 1.0, 50000.0, pair = "BTCUSD")
        val buyOrder2 = LimitOrder("54322", "BUY", 1.0, 49999.0, pair = "BTCUSD")
        val buyOrder3 = LimitOrder("54323", "BUY", 1.0, 49998.0, pair = "BTCUSD")
        val sellOrder = MarketOrder("1", "SELL", baseAmount = 3.1, pair = "BTCUSD")

        callPrivateMethod<Unit>(orderBook, "add", buyOrder)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder2)
        callPrivateMethod<Unit>(orderBook, "add", buyOrder3)
        assertEquals(0.0, callPrivateMethod<Double>(orderBook, "match", sellOrder))

        val bestBid = getPrivateProperty<RedBlackTree.Node<Level>?>(orderBook, "bestBid")
        val bids = getPrivateProperty<RedBlackTree<Level>>(orderBook, "bids").toList(reverse = true) { it.orders() }
        val tradeHistory = callPrivateMethod<List<Trade>>(orderBook, "getTradeHistory", 0, 10)

        assertNull(bestBid)
        assertEquals(emptyList(), bids)

        assertEquals(1, tradeHistory.size)
        assertEquals(49999.0, tradeHistory.first().price)
        assertEquals(sellOrder.baseAmount!! - 0.1, tradeHistory.first().quantity)
        assertEquals(149997.0, tradeHistory.first().quoteVolume)
        assertEquals(buyOrder.pair, tradeHistory.first().currencyPair)
        assertEquals(sellOrder.side, tradeHistory.first().takerSide)
    }

    // Helper functions
    @Suppress("UNCHECKED_CAST")
    private fun <T> getPrivateProperty(instance: Any, propertyName: String): T {
        val property = instance::class.declaredMemberProperties.firstOrNull { it.name == propertyName }
        property?.isAccessible = true
        return (property as KProperty1<Any, T>).get(instance)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> callPrivateMethod(instance: Any, functionName: String, vararg args: Any?): T {
        val function = try {
            instance::class.declaredMemberFunctions.first { it.name == functionName }
        } catch (e: NoSuchElementException) {
            instance::class.superclasses.first { it.simpleName == "BaseOrderBook" }.declaredMemberFunctions.first { it.name == functionName }
        }
        function.isAccessible = true
        return (function).call(instance, *args) as T
    }

    private fun createDefaultLimitOrder(
        id: String? = null,
        side: String = "BUY",
        quantity: Double = 1.0,
        price: Double = 1.0,
        pair: String = "BTCUSD",
    ): LimitOrder {
        return LimitOrder(
            id = id,
            side = side,
            quantity = quantity,
            price = price,
            pair = pair
        )
    }
}
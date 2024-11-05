package smith.adam

import io.ktor.server.application.*
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import smith.adam.orderbook.BaseOrderBook
import smith.adam.orderbook.SimpleOrderBook
import smith.adam.plugin.configureRouting
import smith.adam.plugin.configureSerialization
import smith.adam.service.OrderService
import smith.adam.service.OrderValidationService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val vertx = Vertx.vertx()

    val btcOrderBook = SimpleOrderBook("BTCUSD", 2)
    val ethOrderBook = SimpleOrderBook("ETHUSD", 6)

    deployOrderBookVerticle(vertx, btcOrderBook)
    deployOrderBookVerticle(vertx, ethOrderBook)

    val orderValidationService = OrderValidationService()
    val orderService = OrderService(vertx, mutableSetOf("BTCUSD", "ETHUSD"), orderValidationService)

    configureSerialization()
    configureRouting(orderService = orderService)
}

private fun deployOrderBookVerticle(
    vertx: Vertx,
    orderBook: BaseOrderBook,
) {
    vertx.deployVerticle(orderBook, DeploymentOptions()) { result ->
        if (result.succeeded()) {
            println("${orderBook.pair} OrderBook deployed successfully.")
        } else {
            println("Failed to deploy ${orderBook.pair} OrderBook: ${result.cause()}")
        }
    }
}
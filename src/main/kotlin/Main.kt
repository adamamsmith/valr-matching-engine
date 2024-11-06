package smith.adam

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import smith.adam.orderbook.BaseOrderBook
import smith.adam.orderbook.OrderBook
import smith.adam.orderbook.SimpleOrderBook
import smith.adam.server.HttpServer

val logger: Logger = LoggerFactory.getLogger("Main")

fun main() {
    val vertx = Vertx.vertx()
    val btcOrderBook = OrderBook("BTCUSD", 2)
    val ethOrderBook = SimpleOrderBook("ETHUSD", 6)

    deployOrderBookVerticle(vertx, btcOrderBook)
    deployOrderBookVerticle(vertx, ethOrderBook)

    vertx.deployVerticle(HttpServer(port = 8080), DeploymentOptions()) { result ->
        if (result.succeeded()) {
            logger.info("HttpServer deployed successfully.")
        } else {
            logger.error("Failed to deploy HttpServer: ${result.cause()}")
        }
    }
}

private fun deployOrderBookVerticle(
    vertx: Vertx,
    orderBook: BaseOrderBook,
) {
    vertx.deployVerticle(orderBook, DeploymentOptions()) { result ->
        if (result.succeeded()) {
            logger.info("${orderBook.pair} OrderBook deployed successfully.")
        } else {
            logger.error("Failed to deploy ${orderBook.pair} OrderBook: ${result.cause()}")
        }
    }
}
package smith.adam

import com.charleskorn.kaml.Yaml
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import smith.adam.model.Config
import smith.adam.orderbook.BaseOrderBook
import smith.adam.orderbook.OrderBook
import smith.adam.server.HttpServer
import smith.adam.service.OrderService
import smith.adam.service.OrderValidationService
import java.io.FileNotFoundException

val logger: Logger = LoggerFactory.getLogger("Main")

fun main() {
    val vertx = Vertx.vertx()

    val classLoader = Thread.currentThread().contextClassLoader
    val inputStream = classLoader.getResourceAsStream("config.yaml")
        ?: throw FileNotFoundException("config.yaml not found in resources")
    val configFile = inputStream.bufferedReader().use { it.readText() }

    val config = Yaml.default.decodeFromString(Config.serializer(), configFile)
    val pairs = mutableSetOf<String>()

    config.orderbooks.forEach { pair ->
        pairs.add(pair)
        val orderBook = OrderBook(pair)
        deployOrderBookVerticle(vertx, orderBook)
    }

    val validationService = OrderValidationService(pairs)
    val orderService = OrderService(validationService)

    vertx.deployVerticle(HttpServer(config.server.port, orderService), DeploymentOptions()) { result ->
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
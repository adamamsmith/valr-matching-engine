package smith.adam

import io.ktor.server.application.*
import smith.adam.model.orderbook.BaseOrderBook
import smith.adam.model.orderbook.SimpleOrderBook
import smith.adam.plugin.configureRouting
import smith.adam.plugin.configureSerialization
import smith.adam.service.OrderService
import smith.adam.service.OrderValidationService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val orderBooks: MutableMap<String, BaseOrderBook> = mutableMapOf(
        "BTCUSD" to SimpleOrderBook(),
        "ETHUSD" to SimpleOrderBook()
    )
    val orderValidationService = OrderValidationService()

    val orderService = OrderService(orderBooks, orderValidationService)

    configureSerialization()
    configureRouting(orderService = orderService)
}
package smith.adam

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import smith.adam.model.Order
import smith.adam.service.OrderService

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        val orderService = OrderService()

        routing {
            post("/orders") {
                val orderRequest = call.receive<Order>()
                val orderId = orderService.placeOrder(orderRequest)
                call.respond(HttpStatusCode.Created, orderId)
            }

            delete("/orders/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                if (orderService.cancelOrder(id)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/orderbook") {
                val orderBook = orderService.getOrderBook()

                if (orderBook.isEmpty()) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(orderBook)
                }
            }
        }
    }.start(wait = true)
}

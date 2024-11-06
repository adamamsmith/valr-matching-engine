package smith.adam.orderbook.model

import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val logger: Logger = LoggerFactory.getLogger("OrderBookMessages")

private const val BASE_ADDRESS = "orderbook"

interface JsonSerializable {
    fun toJson(): String
}

@Serializable
class GetBookRequest : JsonSerializable {
    override fun toJson(): String {
        return Json.encodeToString(serializer(), this)
    }

    companion object {
        private const val ADDRESS = "$BASE_ADDRESS.get"

        fun address(pair: String): String {
            return "$ADDRESS.$pair"
        }

        fun fromJson(json: String): GetBookRequest {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class GetBookResponse(val orderBook: Map<String, List<LimitOrder>>) : JsonSerializable {
    override fun toJson(): String {
        return Json.encodeToString(serializer(), this)
    }

    companion object {
        fun fromJson(json: String): GetBookResponse {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class GetTradeHistoryRequest(val offset: Int, val limit: Int) : JsonSerializable {
    override fun toJson(): String {
        return Json.encodeToString(serializer(), this)
    }

    companion object {
        private const val ADDRESS = "$BASE_ADDRESS.history"

        fun address(pair: String): String {
            return "$ADDRESS.$pair"
        }

        fun fromJson(json: String): GetTradeHistoryRequest {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class GetTradeHistoryResponse(val tradeHistory: List<Trade>) : JsonSerializable {
    override fun toJson(): String {
        return Json.encodeToString(serializer(), this)
    }

    companion object {
        fun fromJson(json: String): GetTradeHistoryResponse {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

sealed class OrderRequest : JsonSerializable {
    abstract val type: OrderType

    enum class OrderType {
        MARKET, LIMIT, CANCEL
    }

    companion object {
        private const val TYPE_KEY = "type"

        fun address(pair: String): String {
            return "$BASE_ADDRESS.$pair"
        }

        fun fromJson(json: String): OrderRequest {
            val jsonObject = Json.parseToJsonElement(json).jsonObject

            return when (val type = jsonObject[TYPE_KEY]?.jsonPrimitive?.content) {
                OrderType.LIMIT.name -> CreateLimitOrder.fromJson(jsonObject.toString())
                OrderType.MARKET.name -> CreateMarketOrder.fromJson(jsonObject.toString())
                OrderType.CANCEL.name -> CancelLimitOrder.fromJson(jsonObject.toString())
                else -> throw IllegalArgumentException("Unknown order type: $type")
            }
        }
    }

    @Serializable
    data class CreateLimitOrder(
        val order: LimitOrder,
        override val type: OrderType = OrderType.LIMIT
    ) : OrderRequest() {
        override fun toJson(): String {
            val withDefaults = Json { encodeDefaults = true }
            return withDefaults.encodeToString(serializer(), this)
        }

        companion object {
            fun fromJson(json: String): CreateLimitOrder {
                return Json.decodeFromString(serializer(), json)
            }
        }
    }

    @Serializable
    data class CreateMarketOrder(
        val order: MarketOrder,
        override val type: OrderType = OrderType.MARKET
    ) : OrderRequest() {
        override fun toJson(): String {
            val withDefaults = Json { encodeDefaults = true }
            return withDefaults.encodeToString(serializer(), this)
        }

        companion object {
            fun fromJson(json: String): CreateMarketOrder {
                return Json.decodeFromString(serializer(), json)
            }
        }
    }

    @Serializable
    data class CancelLimitOrder(
        val order: CancelOrder,
        override val type: OrderType = OrderType.CANCEL
    ) : OrderRequest() {
        override fun toJson(): String {
            val withDefaults = Json { encodeDefaults = true }
            return withDefaults.encodeToString(serializer(), this)
        }

        companion object {
            fun fromJson(json: String): CancelLimitOrder {
                return Json.decodeFromString(serializer(), json)
            }
        }
    }
}
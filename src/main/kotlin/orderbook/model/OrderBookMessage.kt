package smith.adam.orderbook.model

private const val BASE_ADDRESS = "orderbook"

class GetBookRequest {
    companion object {
        private const val ADDRESS = "$BASE_ADDRESS.get"

        fun address(pair: String): String {
            return "$ADDRESS.$pair"
        }
    }
}

data class GetTradeHistoryRequest(val offset: Int, val limit: Int) {
    companion object {
        private const val ADDRESS = "$BASE_ADDRESS.history"

        fun address(pair: String): String {
            return "$ADDRESS.$pair"
        }
    }
}

data class GetBookResponse(val orderBook: Map<String, List<LimitOrder>>)
data class GetTradeHistoryResponse(val tradeHistory: List<Trade>)

sealed class OrderRequest {
    companion object {
        fun address(pair: String): String {
            return "$BASE_ADDRESS.$pair"
        }
    }

    data class CreateLimitOrder(val limitOrder: LimitOrder) : OrderRequest()
    data class CreateMarketOrder(val marketOrder: MarketOrder) : OrderRequest()
    data class CancelLimitOrder(val cancelOrder: CancelOrder) : OrderRequest()
}
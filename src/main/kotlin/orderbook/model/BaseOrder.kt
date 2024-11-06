package smith.adam.orderbook.model


abstract class BaseOrder {
    abstract val id: String?
    abstract val side: String
    abstract val price: Double?
    abstract val baseAmount: Double?
    abstract val quoteAmount: Double?
    abstract val pair: String
}
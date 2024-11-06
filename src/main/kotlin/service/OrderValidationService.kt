package smith.adam.service

import io.vertx.ext.web.handler.HttpException
import smith.adam.orderbook.model.CancelOrder
import smith.adam.orderbook.model.LimitOrder
import smith.adam.orderbook.model.MarketOrder
import smith.adam.orderbook.model.Side

class OrderValidationService(private val validCurrencyPairs: MutableSet<String>) {
    fun validate(marketOrder: MarketOrder) {
        buildBadRequestException(
            listOf(
                validateCurrencyPair(marketOrder.pair), validateMarketOrderAmount(marketOrder)
            )
        )
    }

    fun validate(limitOrder: LimitOrder) {
        buildBadRequestException(
            listOf(
                validateCurrencyPair(limitOrder.pair),
                validateSide(limitOrder),
                validateAmount("price", limitOrder.price),
                validateAmount("quoteAmount", limitOrder.quoteAmount)

            )
        )
    }

    fun validate(cancelOrder: CancelOrder) {
        buildBadRequestException(
            listOf(
                validateCurrencyPair(cancelOrder.pair)
            )
        )
    }

    fun validate(currencyPair: String?) {
        buildBadRequestException(
            listOf(
                validateCurrencyPair(currencyPair)
            )
        )
    }

    private fun validateCurrencyPair(currencyPair: String?): String {
        if (currencyPair == null) return "Market must be provided"
        return if (currencyPair !in validCurrencyPairs) "Market does not exist for pair $currencyPair" else ""
    }

    private fun validateMarketOrderAmount(marketOrder: MarketOrder): String {
        return when (Side.fromString(marketOrder.side)) {
            Side.BUY -> validateAmount("quoteAmount", marketOrder.quoteAmount, " when side is BUY")
            Side.SELL -> validateAmount("baseAmount", marketOrder.quoteAmount, " when side is SELL")
            null -> "Invalid side: ${marketOrder.side}"
        }
    }

    private fun validateAmount(name: String, amount: Double?, extra: String? = null): String {
        return if (amount == null || amount <= 0.0) "$name must be non-negative${extra ?: ""}" else ""
    }

    private fun validateSide(limitOrder: LimitOrder): String {
        return when (Side.fromString(limitOrder.side)) {
            Side.BUY -> ""
            Side.SELL -> ""
            null -> "Invalid side: ${limitOrder.side}"
        }
    }

    private fun buildBadRequestException(messages: List<String>) {
        val filteredMessages = messages.filter { it != "" }
        if (filteredMessages.isNotEmpty()) throw HttpException(
            400, filteredMessages.joinToString(
                separator = ". ", postfix = "."
            )
        )
    }
}
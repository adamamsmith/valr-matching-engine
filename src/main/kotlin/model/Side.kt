package smith.adam.model

enum class Side {
    BUY,
    SELL;

    companion object {
        fun fromString(value: String): Side? {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}
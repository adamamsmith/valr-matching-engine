package smith.adam.model

sealed class OrderEvent {
    data class CreateOrder(val order: Order) : OrderEvent()
    data class DeleteOrder(val id: String) : OrderEvent()
}
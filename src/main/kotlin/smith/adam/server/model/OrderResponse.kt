package smith.adam.server.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OrderResponse(val id: String) {
    companion object {
        fun fromJson(json: String): OrderResponse {
            return Json.decodeFromString(serializer(), json)
        }
    }
}
package smith.adam.server.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ErrorResponse(val status: Int, val reason: String) {
    companion object {
        fun fromJson(json: String): ErrorResponse {
            return Json.decodeFromString(serializer(), json)
        }
    }
}
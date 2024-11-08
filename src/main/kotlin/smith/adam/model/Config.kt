package smith.adam.model

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val server: ServerConfig,
    val orderbooks: List<String>
)

@Serializable
data class ServerConfig(
    val port: Int
)
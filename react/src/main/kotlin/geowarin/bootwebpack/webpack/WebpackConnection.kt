package geowarin.bootwebpack.webpack

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Component
open class WebpackConnection constructor(private var assetStore: AssetStore) : ApplicationListener<ContextRefreshedEvent> {

    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent) {

        val socket = IO.socket("http://localhost:3000")
        socket
                .on(Socket.EVENT_CONNECT) { args ->
                    println("Connected")
                }
                .on("wp-emit") { args ->
                    val Json: JSONArray = args[0] as JSONArray
                    val assets = toAssetList(Json)
                    assetStore.store(assets)
                }
                .on(Socket.EVENT_DISCONNECT) { args ->
                    println("disco")
                }
        socket.connect()
    }
}

data class Asset(val name: String, val source: String)

fun toAssetList(jsonArray: JSONArray): List<Asset> {
    return (0..(jsonArray.length() - 1))
            .map { jsonArray.getJSONObject(it) }
            .map {
                Asset(
                        name = it.getString("name").removePrefix("/"),
                        source = it.getString("source")
                )
            }
}
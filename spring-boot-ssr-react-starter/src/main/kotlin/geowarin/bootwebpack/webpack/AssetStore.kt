package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8TypedArray
import com.eclipsesource.v8.V8Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets


@Component
open class AssetStore {
    var assets: MutableMap<String, Asset> = mutableMapOf<String, Asset>()

    fun store(assets: List<Asset>) {
//        this.assets.clear()
        assets.forEach { this.assets.put(it.name, it) }
    }

    fun getAsset(requestPath: String): Asset? {
        return assets[requestPath]
    }

    fun getCssNames(): List<String> {
        return assets.keys.filter { it.endsWith(".css") }
    }

    fun hasAsset(requestPath: String): Boolean {
        return assets.contains(requestPath)
    }

    fun getAssetAsResource(requestPath: String, modulePath: String?): Resource? {
        val asset = getAsset(requestPath)
        if (asset != null) {
            if (modulePath != null) {
                val prefix = modulePath + " = "
                val source = String(asset.source)
                return WebpackResource((prefix + source).toByteArray(), asset.name)
            } else {
                return WebpackResource(asset.source, asset.name)
            }
        }
        return null
    }

    fun getAssetSource(requestPath: String): String? {
        val asset = getAsset(requestPath)
        if (asset != null) {
            return String(asset.source)
        }
        return null
    }
}

class WebpackResource(byteArray: ByteArray?, val fileName: String) : ByteArrayResource(byteArray) {

    override fun getFilename(): String {
        return fileName
    }

    override fun lastModified(): Long {
        return 0
    }
}

data class Asset(val name: String, val source: ByteArray) {
    constructor(obj: V8Object) : this(name = obj.getString("name"), source = getSourceAsByteArray(obj))
}

fun getSourceAsByteArray(obj: V8Object): ByteArray {
    val sourceObj = obj.get("source")
    try {
        when (sourceObj) {
            is V8TypedArray -> {
                return toByteArray(sourceObj.byteBuffer)
            }
            is String -> {
                return sourceObj.toByteArray(StandardCharsets.UTF_8)
            }
        }
    } finally {
        if (sourceObj is V8Value) {
            sourceObj.release()
        }
    }

    throw IllegalStateException("Unexpected source content")
}

fun toByteArray(buffer: ByteBuffer): ByteArray {
    val bytes: ByteArray = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}
package geowarin.bootwebpack.webpack

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
open class AssetStore {
    var assets: MutableMap<String, Asset> = mutableMapOf<String, Asset>()

    fun store(assets: List<Asset>) {
        assets.forEach { this.assets.put(it.name, it) }
    }

    fun getAsset(requestPath: String): Asset? {
        return assets.get(requestPath)
    }

    fun getAssetAsResource(requestPath: String, modulePath: String?): Resource? {
        val asset = getAsset(requestPath)
        if (asset != null) {
            val prefix = if (modulePath != null) modulePath + " = " else ""
            return WebpackResource((prefix + asset.source).toByteArray(), asset.name)
        }
        return null
    }

    fun getAssetSource(requestPath: String): String? {
        val asset = getAsset(requestPath)
        if (asset != null) {
            return asset.source
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

data class Asset(val name: String, val source: String)
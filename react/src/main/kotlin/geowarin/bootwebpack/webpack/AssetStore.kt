package geowarin.bootwebpack.webpack

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
open class AssetStore {
    var assets: MutableList<Asset> = mutableListOf()

    init {
        addBuiltinAsset("client.js")
        addBuiltinAsset("renderer.js")
        addBuiltinAsset("vendors.js")
    }

    private fun addBuiltinAsset(assetName: String) {
        assets.add(
                Asset(
                        name= assetName,
                        source = ClassPathResource("scripts/$assetName").file.readText()
                )

        )
    }

    fun store(assets: List<Asset>) {
//        this.assets = assets
    }

    fun getAsset(requestPath: String): Asset? {
        return assets
                .find { it.name == requestPath }
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
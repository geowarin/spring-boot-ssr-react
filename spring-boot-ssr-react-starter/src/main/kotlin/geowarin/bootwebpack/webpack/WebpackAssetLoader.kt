package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.extensions.resource.readBytes
import geowarin.bootwebpack.extensions.resource.relativizePath
import mu.KotlinLogging
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver


class WebpackAssetLoader {

    private val logger = KotlinLogging.logger {}

    fun loadAssetsFromLocation(assetStore: AssetStore, webpackAssetsLocation: String?) {
        val root = ClassPathResource("/$webpackAssetsLocation")

        val pattern = "/$webpackAssetsLocation/**/*"
        val webpackResources = PathMatchingResourcePatternResolver().getResources(pattern)

        val allAssets = webpackResources
                .filter { it.isReadable }
                .map { Asset(name = it.relativizePath(root), source = it.readBytes()) }

        logger.debug { "Added ${allAssets.size} webpack assets to the store: ${allAssets.map { it.name }.joinToString()}" }
        assetStore.store(allAssets)
    }
}
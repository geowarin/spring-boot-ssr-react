package geowarin.bootwebpack.webpack

import org.amshove.kluent.shouldEqual
import org.junit.Assert.*
import org.junit.Test

class WebpackAssetLoaderTest {

    @Test
    fun should_load_any_resources() {

        val assetStore = AssetStore()
        WebpackAssetLoader().loadAssetsFromLocation(assetStore, "fonts")

        assetStore.assets.values.map { it.name } shouldEqual listOf("fontello/fontello-webfont.woff2", "fontello/fontello.css", "styled.js")
    }
}
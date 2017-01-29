package geowarin.bootwebpack.webpack

import org.amshove.kluent.shouldEqual
import org.junit.Test


class AssetStoreTest {
    @Test
    fun should_remove_hash() {

        val assetStore = AssetStore()
        assetStore.store(listOf(
                Asset(name = "fonts/fontello-webfont-c810fb1e81981ca125bbe59a5dd306b4.woff", source = "woff".toByteArray()),
                Asset(name = "Dashboard__f8fb602f589ba82c6171__.js", source = "dash".toByteArray()),
                Asset(name = "sub/Projects__b12d5724621518781525__.js", source = "proj".toByteArray())
        ))

        // not a valid resource hash (those are handled by webpack automatically)
        assetStore.getAssetSourceByChunkName("fonts/fontello-webfont.woff") shouldEqual null

        assetStore.getAssetSourceByChunkName("Dashboard.js") shouldEqual "dash"
        assetStore.getAssetSourceByChunkName("sub/Projects.js") shouldEqual "proj"
    }
}
package geowarin.bootwebpack.webpack

import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import java.io.File


open class WebpackWatcher(val assetStore: AssetStore) : ApplicationListener<ContextRefreshedEvent> {

    companion object {
        //        @Synchronized
        var INITIALIZED = false
    }

    override fun onApplicationEvent(event: ContextRefreshedEvent) {

        if (!INITIALIZED) {
            INITIALIZED = true
            watch()
        }
    }

    fun watch() {
        val pagesDir = File("/Users/geowarin/dev/projects/boot-wp/demo/src/main/js/pages")
        val pages = pagesDir.walk().filter { f -> f.isFile }.toList()
        val compiler = WebpackCompiler(
                pages = pages,
                bootSsrDirectory = File("/Users/geowarin/dev/projects/boot-wp/react/boot-ssr")
        )
        val watchDirectories = arrayOf(
                File("/Users/geowarin/dev/projects/boot-wp/demo/src/main/js/")
        )
        compiler.watchAsync(*watchDirectories).forEach { res -> assetStore.store(res.assets) }
    }
}
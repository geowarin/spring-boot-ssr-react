package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.config.ReactSsrProperties
import geowarin.bootwebpack.config.WebpackOptionFactory
import mu.KotlinLogging
import org.springframework.boot.ApplicationHome
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import java.io.File

open class WebpackWatcher(val assetStore: AssetStore, val properties: ReactSsrProperties) : ApplicationListener<ApplicationReadyEvent> {
    private val logger = KotlinLogging.logger {}

    companion object {
        var INITIALIZED = false
    }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        // Do not reload if the devtools trigger a refresh
        if (!INITIALIZED) {
            INITIALIZED = true
            val projectDir = getProjectDir(event.springApplication.mainApplicationClass)
            watch(projectDir)
        }
    }

    fun watch(projectDir: File) {
        val options = WebpackOptionFactory().create(projectDir.toPath(), properties)
        WebpackCompiler().watchAsync(options).forEach { res ->
            run {
                // TODO: check errors
                logger.info { "${res.assets.size} webpack assets compiled in ${res.compileTime}ms" }
                assetStore.store(res.assets)
            }
        }
    }

    fun getProjectDir(mainApplicationClass: Class<*>): File {
        // TODO: simplify this
        val dir = ApplicationHome(mainApplicationClass).dir
        if (dir.toPath().endsWith("target/classes")) {
            // maven
            return dir.parentFile.parentFile
        } else if (dir.toPath().endsWith("build/classes/main")) {
            // gradle
            return dir.parentFile.parentFile.parentFile
        } else {
            // fallback
            return File(System.getProperty("user.dir"))
        }
    }
}

package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.config.ReactSsrProperties
import geowarin.bootwebpack.extensions.pathWithoutExt
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

        val jsSourceDir = File(projectDir, properties.jsSourceDirectory)
        if (!jsSourceDir.exists()) {
            throw IllegalStateException("Could not find js source directory ${jsSourceDir.canonicalPath}")
        }

        val bootSsrNodeModulePath: File
        if (File(properties.bootSsrNodeModulePath).isAbsolute) {
            bootSsrNodeModulePath = File(properties.bootSsrNodeModulePath)
        } else {
            bootSsrNodeModulePath = File(jsSourceDir, properties.bootSsrNodeModulePath)
        }
        if (!bootSsrNodeModulePath.exists()) {
            throw IllegalStateException("Could not find the path to the companion node_module ${bootSsrNodeModulePath.canonicalPath}")
        }

        val pagesDir = File(jsSourceDir, properties.pageDir)
        if (!pagesDir.exists()) {
            logger.warn { "Pages dir does not exist!" }
        }

        val pagesFile = pagesDir.walk().filter { f -> f.isFile }.toList()
        val pages = pagesFile.map { Page(name = it.relativeTo(pagesDir).pathWithoutExt(), file = it) }

        logger.info { "Found ${pages.size} react pages" }

        if (pages.isEmpty()) {
            logger.warn { "No pages where found in ${pagesDir.canonicalPath}. You should add at least one React component in there" }
        }

        val compiler = WebpackCompiler(
                bootSsrDirectory = bootSsrNodeModulePath
        )
        val options = Options(
                pages = pages,
                watchDirectories = listOf(jsSourceDir.canonicalPath)
        )
        compiler.watchAsync(options).forEach { res ->
            run {
                logger.info { "${res.assets.size} webpack assets compiled in ${res.compileTime}ms" }
                assetStore.store(res.assets)
            }
        }
    }

    fun getProjectDir(mainApplicationClass: Class<*>): File {
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

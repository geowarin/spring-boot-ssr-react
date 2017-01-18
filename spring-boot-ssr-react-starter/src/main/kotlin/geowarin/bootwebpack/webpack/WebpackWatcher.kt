package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.config.BootSsrOptions
import geowarin.bootwebpack.config.ReactSsrProperties
import geowarin.bootwebpack.config.WebpackOptionFactory
import geowarin.bootwebpack.files.WatchEventObservable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
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
        val webpackOptionFactory = WebpackOptionFactory()
        val options = webpackOptionFactory.create(projectDir.toPath(), properties)

        val pagesDir = options.additionalBuildInfo.pagesDir

        val webpackCompiler = WebpackCompiler()
        var subscription: Disposable? = null

        WatchEventObservable
                .addAndDeleteWatcher(pagesDir)
                .subscribeOn(Schedulers.io())
                .forEach {
                    subscription = restartWebpackCompiler(options, subscription, webpackCompiler)
                }

        subscription = listenToWebpack(webpackCompiler, options.webpackCompilerOptions)
    }

    private fun restartWebpackCompiler(options: BootSsrOptions, subscription: Disposable?, webpackCompiler: WebpackCompiler): Disposable {
        logger.info { "Pages added or removed, relaunching webpack" }

        subscription?.dispose()
        webpackCompiler.stop()

        val newPages = WebpackOptionFactory().getPages(options.additionalBuildInfo.pagesDir)
        val compilerOptions = options.webpackCompilerOptions.copy(pages = newPages)

        return listenToWebpack(webpackCompiler, compilerOptions)
    }

    private fun listenToWebpack(webpackCompiler: WebpackCompiler, compilerOptions: WebpackCompilerOptions): Disposable {
        return webpackCompiler
                .watchAsync(compilerOptions)
                .subscribeOn(Schedulers.single())
                .forEach(this::onCompilationResult)
    }

    fun onCompilationResult(res: CompilationResult) {

        if (res.hasErrors()) {
            val error = res.errors.first()
            logger.error { "\n" + error.message }
        }

        val assets = res.assets
        logger.info { "${assets.size} webpack assets compiled in ${res.compileTime}ms" }
        assetStore.store(assets)
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
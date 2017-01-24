package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.config.BootSsrConfiguration
import geowarin.bootwebpack.config.BootSsrConfigurationFactory
import geowarin.bootwebpack.files.WatchEventObservable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import java.nio.charset.StandardCharsets

typealias Refresher = () -> Unit

open class WebpackWatcher(val assetStore: AssetStore, val configurationFactory: BootSsrConfigurationFactory, val refresher: Refresher) : ApplicationListener<ApplicationReadyEvent> {
    private val logger = KotlinLogging.logger {}
    private var vendorsManifest: Asset? = null

    companion object {
        var INITIALIZED = false
    }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        // Do not reload if the devtools trigger a refresh
        if (!INITIALIZED) {
            INITIALIZED = true
            val config = configurationFactory.create()
            if (config.additionalBuildInfo.enableDll) {
                generateDll(config)
            }
            watch(config)
        }
    }

    private fun generateDll(config: BootSsrConfiguration) {

        logger.info { "Generating DLL..." }
        val dllCompilation = DefaultWebpackCompiler().generateDll(config.webpackCompilerOptions)
        val vendors = dllCompilation.assets.find { it.name == "vendors.dll.js" } ?: throw IllegalStateException()
        assetStore.store(listOf(vendors))
        vendorsManifest = dllCompilation.assets.find { it.name == "vendors.manifest.json" }
        logger.info { "Generated DLL in ${dllCompilation.compileTime}ms" }

//        WebpackCompilationWriter().write(dllCompilation, config.additionalBuildInfo.jsSourceDir / "dll")
    }

    fun watch(config: BootSsrConfiguration) {
        val pagesDir = config.additionalBuildInfo.pagesDir

        val webpackCompiler = DefaultWebpackCompiler()
        var subscription: Disposable? = null

        WatchEventObservable
                .addAndDeleteWatcher(pagesDir)
                .subscribeOn(Schedulers.io())
                .forEach {
                    subscription = restartWebpackCompiler(config, subscription, webpackCompiler)
                }

        val compilerOptions = config.webpackCompilerOptions.copy(
                dllManifestContent = vendorsManifest?.source?.toString(StandardCharsets.UTF_8)
        )
        subscription = listenToWebpack(webpackCompiler, compilerOptions)
    }

    private fun restartWebpackCompiler(configuration: BootSsrConfiguration, subscription: Disposable?, webpackCompiler: WebpackCompiler): Disposable {
        logger.info { "Pages added or removed, relaunching webpack" }

        subscription?.dispose()
        webpackCompiler.stop()

        val newPages = configurationFactory.getPages(configuration.additionalBuildInfo.pagesDir)
        val compilerOptions = configuration.webpackCompilerOptions.copy(
                pages = newPages,
                dllManifestContent = vendorsManifest?.source?.toString(StandardCharsets.UTF_8)
        )

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
            // todo: display more info (source, stack)
            logger.error { "\n" + error.message }
        }

        val assets = res.assets
        logger.info { "${assets.size} webpack assets compiled in ${res.compileTime}ms" }
        assetStore.store(assets)

        refresher()
    }
}

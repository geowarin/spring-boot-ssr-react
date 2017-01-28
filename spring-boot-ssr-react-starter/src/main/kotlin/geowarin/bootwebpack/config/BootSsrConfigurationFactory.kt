package geowarin.bootwebpack.config

import geowarin.bootwebpack.extensions.path.*
import geowarin.bootwebpack.webpack.Page
import geowarin.bootwebpack.webpack.WebpackCompilerOptions
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import java.nio.file.Path

data class BootSsrConfiguration(
        val webpackCompilerOptions: WebpackCompilerOptions,
        val additionalBuildInfo: AdditionalBuildInfo
)

data class AdditionalBuildInfo(
        val pagesDir: Path,
        val projectDir: Path,
        val jsSourceDir: Path,
        val webpackAssetsLocation: String,
        val enableDll: Boolean
) {
    fun distDir(assetDestination: Path): Path {
        return assetDestination / webpackAssetsLocation
    }
}

class BootSsrConfigurationFactory(
        val properties: ReactSsrProperties,
        val projectDirResolver: ProjectDirResolver = DefaultProjectDirResolver(),
        val shouldCheckPaths:Boolean = true
) : ApplicationListener<ApplicationReadyEvent> {
    lateinit var projectDir: Path

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val mainApplicationClass = event.springApplication.mainApplicationClass
        val projectDirectory = projectDirResolver.resolveProjectDirectory(mainApplicationClass)
        initializeProjectDir(projectDirectory)
    }

    // For tests
    fun initializeProjectDir(projectDirectory: Path) {
        this.projectDir = projectDirectory
    }

    private val logger = KotlinLogging.logger {}

    fun create(): BootSsrConfiguration {
        val jsSourceDir = checkJsSourceDir(projectDir / properties.jsSourceDirectory)
        val bootSsrNodeModulePath = checkNodeModulePath(jsSourceDir, properties.bootSsrNodeModulePath)

        val pagesDirPath = jsSourceDir / properties.pageDir
        val pagesDir = checkPagesDirectory(pagesDirPath)

        val pages = getPages(pagesDir)

        val webpackCompilerOptions = WebpackCompilerOptions(
                bootSsrDirectory = bootSsrNodeModulePath,
                projectDirectory = jsSourceDir,
                pages = pages,
                additionalDllLibs = properties.additionalDllLibs,
                watchDirectories = listOf(jsSourceDir),
                minify = properties.build.isMinify,
                generateStats = properties.build.isGenerateStats
        )
        val additionalBuildInfo = AdditionalBuildInfo(
                pagesDir = pagesDir,
                projectDir = projectDir,
                jsSourceDir = jsSourceDir,
                webpackAssetsLocation = properties.webpackAssetsLocation,
                enableDll = properties.isEnableDll
        )
        return BootSsrConfiguration(webpackCompilerOptions, additionalBuildInfo)
    }

    fun getPages(pagesDir: Path): List<Page> {
        val pagesFile = pagesDir.walk().filter { f -> f.isRegularFile && f.fileExtension == "js" }.toList()

        val pages = pagesFile.map { Page(name = relativize(it, pagesDir), path = it) }

        logger.info { "Found ${pages.size} react pages" }

        if (pages.isEmpty()) {
            logger.warn { "No pages where found in $pagesDir. You should add at least one React component in there" }
        }
        return pages
    }

    private fun relativize(page: Path, pagesDir: Path) = withoutExt(pagesDir.relativize(page))

    private fun withoutExt(path: Path) = path.toString().replaceAfterLast(".", "").dropLast(1)

    fun checkPagesDirectory(pagesDirPath: Path): Path {
        if (shouldCheckPaths && !pagesDirPath.exists) {
            logger.warn { "Pages dir does not exist!" }
        }
        return pagesDirPath
    }

    fun checkNodeModulePath(jsSourceDir: Path, bootSsrNodeDir: String): Path {

        val bootSsrNodePath = jsSourceDir.fileSystem.getPath(bootSsrNodeDir)
        val bootSsrNodeModulePath: Path
        if (bootSsrNodePath.isAbsolute) {
            bootSsrNodeModulePath = bootSsrNodePath
        } else {
            bootSsrNodeModulePath = jsSourceDir / bootSsrNodePath
        }
        if (shouldCheckPaths && bootSsrNodeModulePath.notExists) {
            throw IllegalStateException("Could not find the path to the companion node_module $bootSsrNodeModulePath")
        }
        return bootSsrNodeModulePath
    }

    fun checkJsSourceDir(jsSourceDir: Path): Path {
        if (shouldCheckPaths && jsSourceDir.notExists) {
            throw IllegalStateException("Could not find js source directory $jsSourceDir")
        }
        return jsSourceDir
    }
}
package geowarin.bootwebpack.config

import geowarin.bootwebpack.extensions.path.*
import geowarin.bootwebpack.webpack.Page
import geowarin.bootwebpack.webpack.WebpackCompilerOptions
import mu.KotlinLogging
import java.nio.file.Path

class WebpackOptionFactory {
    private val logger = KotlinLogging.logger {}

    fun create(projectDir: Path, properties: ReactSsrProperties): WebpackCompilerOptions {
        // TODO: check if production

        val jsSourceDir = checkJsSourceDir(projectDir / properties.jsSourceDirectory)
        val bootSsrNodeModulePath = checkNodeModulePath(jsSourceDir, properties.bootSsrNodeModulePath.toPath())

        val pagesDirPath = jsSourceDir / properties.pageDir
        val pagesDir = checkPagesDirectory(pagesDirPath)

        val pages = getPages(pagesDir)

        return WebpackCompilerOptions(
                bootSsrDirectory = bootSsrNodeModulePath.toFile(),
                pages = pages,
                watchDirectories = listOf(jsSourceDir)
        )
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
        if (!pagesDirPath.exists) {
            logger.warn { "Pages dir does not exist!" }
        }
        return pagesDirPath
    }

    fun checkNodeModulePath(jsSourceDir: Path, bootSsrNodePath: Path): Path {

        val bootSsrNodeModulePath: Path
        if (bootSsrNodePath.isAbsolute) {
            bootSsrNodeModulePath = bootSsrNodePath
        } else {
            bootSsrNodeModulePath = jsSourceDir / bootSsrNodePath
        }
        if (bootSsrNodeModulePath.notExists) {
            throw IllegalStateException("Could not find the path to the companion node_module $bootSsrNodeModulePath")
        }
        return bootSsrNodeModulePath
    }

    fun checkJsSourceDir(jsSourceDir: Path): Path {
        if (jsSourceDir.notExists) {
            throw IllegalStateException("Could not find js source directory $jsSourceDir")
        }
        return jsSourceDir
    }
}
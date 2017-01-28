package geowarin.bootwebpack.utils

import geowarin.bootwebpack.extensions.path.fileNameWithoutExtension
import geowarin.bootwebpack.extensions.path.toPath
import geowarin.bootwebpack.webpack.Asset
import geowarin.bootwebpack.webpack.CompilationResult
import geowarin.bootwebpack.webpack.Page
import geowarin.bootwebpack.webpack.WebpackCompilerOptions
import geowarin.bootwebpack.webpack.errors.ErrorLogger
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldEqualTo
import org.amshove.kluent.shouldStartWith
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun pageOptions(vararg pagePaths: String): WebpackCompilerOptions {
    val pages = pagePaths.map { ClassPathResource(it).file.toPath() }
    return WebpackCompilerOptions(
            bootSsrDirectory = "../spring-boot-ssr-react-node".toPath(),
            projectDirectory = pages.first().parent,
            pages = toPages(*pages.toTypedArray())
    )
}

fun pageOptions(projectDir: Path, vararg pagePaths: String): WebpackCompilerOptions {
    val pages = pagePaths.map { ClassPathResource(it).file.toPath() }
    return WebpackCompilerOptions(
            bootSsrDirectory = "../spring-boot-ssr-react-node".toPath(),
            projectDirectory = projectDir,
            pages = toPages(*pages.toTypedArray())
    )
}

fun toPages(vararg pagePaths: Path): List<Page> {
    return pagePaths.map { Page(it, it.fileNameWithoutExtension) }
}

infix fun CompilationResult.shouldContainAssets(assets: Iterable<String>?) {
    if (this.hasErrors()) {
        ErrorLogger().displayErrors(errors)
        throw AssertionError("Compilation should be successful")
    }
    val assetsNames = this.assets.map { it.name }.sorted()
    assetsNames shouldEqual assets
}

fun CompilationResult.source(assetName: String): String {
    if (this.hasErrors()) {
        ErrorLogger().displayErrors(errors)
        throw AssertionError("Compilation should be successful")
    }
    val asset = this.assets.find { it.name == assetName }
    val bytes = asset?.source ?: throw IllegalStateException("Could not find asset $assetName")
    return String(bytes)
}

fun CompilationResult.source(predicate: (Asset) -> Boolean): String {
    if (this.hasErrors()) {
        throw AssertionError("Compilation should be successful: " + this.errors.first().message)
    }
    val asset = this.assets.find(predicate)
    val bytes = asset?.source ?: throw IllegalStateException("Could not find asset")
    return String(bytes)
}

infix fun CompilationResult.shouldHaveError(errorMessage: String) {
    if (!this.hasErrors()) {
        throw AssertionError("Got errors")
    }
    this.errors.size shouldEqualTo 1
    val errorsLogs = ErrorLogger().getErrorsLogs(errors)
    errorsLogs.first() shouldEqual errorMessage
}

fun String.asTmpFile(): File {
    val tempFile = createTempFile()
    Files.newBufferedWriter(tempFile.toPath()).use { writer -> writer.write(this) }
    tempFile.deleteOnExit()
    return tempFile
}
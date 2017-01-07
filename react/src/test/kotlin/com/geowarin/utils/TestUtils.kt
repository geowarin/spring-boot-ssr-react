package com.geowarin.utils

import geowarin.bootwebpack.webpack.CompilationResult
import geowarin.bootwebpack.webpack.WebpackCompiler
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldEqualTo
import org.amshove.kluent.shouldStartWith
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.nio.file.Files

fun createTestCompiler(vararg pagePaths: String): WebpackCompiler {
    val pages = pagePaths.map { ClassPathResource(it).file }
    val compiler = WebpackCompiler(
            pages = pages,
            bootSsrDirectory = File("/Users/geowarin/dev/projects/boot-wp/react/boot-ssr")
    )
    return compiler
}

fun createTestCompiler(vararg pages: File): WebpackCompiler {
    val compiler = WebpackCompiler(
            pages = listOf(*pages),
            bootSsrDirectory = File("/Users/geowarin/dev/projects/boot-wp/react/boot-ssr")
    )
    return compiler
}

infix fun CompilationResult.shouldContainAssets(assets: Iterable<String>?) {
    if (this.hasErrors()) {
        throw AssertionError("Compilation should be successful")
    }
    val assetsNames = this.assets.map { it.name }
    assetsNames shouldEqual assets
}

fun CompilationResult.source(assetName: String):String {
    val asset = this.assets.find { it.name == assetName }
    return asset?.source ?: throw IllegalStateException("Could not find asset ${assetName}")
}

infix fun CompilationResult.shouldHaveError(errorMessage: String) {
    if (!this.hasErrors()) {
        throw AssertionError("Got errors")
    }
    this.errors.map { it.message }.size shouldEqualTo 1
    this.errors[0].message shouldStartWith errorMessage
}

fun String.asFile(): File {
    val tempFile = createTempFile()
    Files.newBufferedWriter(tempFile.toPath()).use { writer -> writer.write(this) }
    tempFile.deleteOnExit()
    return tempFile
}
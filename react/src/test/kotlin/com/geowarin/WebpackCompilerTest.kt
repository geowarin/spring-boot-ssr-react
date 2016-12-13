package com.geowarin

import geowarin.bootwebpack.webpack.CompilationError
import geowarin.bootwebpack.webpack.CompilationSuccess
import geowarin.bootwebpack.webpack.WebpackCompiler
import org.amshove.kluent.shouldEqual
import org.junit.Test
import java.io.File

class WebpackCompilerTest {

    @Test
    fun compilation_succeeds() {
        val compiler = WebpackCompiler(
                userProject = File("/Users/geowarin/dev/projects/boot-wp/demo/src/main/js"),
                bootSsrDirectory = File("/Users/geowarin/dev/projects/boot-wp/react/boot-ssr/")
        )
        val compilation = compiler.compile()


        when (compilation) {
            is CompilationSuccess -> {
                val assetsNames = compilation.assets.map { it.name }
                assetsNames shouldEqual listOf("client.js", "renderer.js", "home.js", "common.js")
            }
            is CompilationError -> throw AssertionError("Got errors:\n${compilation.errorMessages.joinToString("\n")})}")
            else -> throw AssertionError("Got errors")
        }
    }
}

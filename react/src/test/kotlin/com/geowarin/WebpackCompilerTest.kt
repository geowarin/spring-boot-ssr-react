package com.geowarin

import com.geowarin.utils.createTestCompiler
import com.geowarin.utils.shouldContainAssets
import com.geowarin.utils.shouldHaveError
import org.junit.Test

class WebpackCompilerTest {

    @Test
    fun compilation_succeeds() {
        val compiler = createTestCompiler("home.js")
        val compilation = compiler.compile()

        compilation shouldContainAssets listOf("client.js", "renderer.js", "home.js", "common.js")
    }

    @Test
    fun compilation_errors() {
        val compiler = createTestCompiler("syntaxError.js")
        val compilation = compiler.compile()

        compilation shouldHaveError "Module build failed: SyntaxError: Unexpected token"
    }
}
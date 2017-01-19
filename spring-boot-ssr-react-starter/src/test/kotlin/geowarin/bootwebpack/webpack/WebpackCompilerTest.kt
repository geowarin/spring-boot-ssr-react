package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.utils.pageOptions
import geowarin.bootwebpack.utils.shouldContainAssets
import geowarin.bootwebpack.utils.shouldHaveError
import geowarin.bootwebpack.utils.source
import org.amshove.kluent.shouldEqual
import org.junit.Test

class WebpackCompilerTest {

    @Test
    fun compilation_succeeds() {
        val compilation = WebpackCompiler().compile(pageOptions("home.js"))

        compilation shouldContainAssets listOf("client.js", "common.js", "home.js", "renderer.js")
        assert(compilation.compileTime > 0, { -> "Should have a compile time" })
    }

    @Test
    fun compilation_postcss() {
        val compilation = WebpackCompiler().compile(pageOptions("css/styled.js"))

        val source = compilation.source { it.name.endsWith("css") }
        println(source)
        source shouldEqual """div {
    color: red;
    size: calc(12px * 2);
}

body div.myClass {
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
}
"""
    }

    @Test
    fun compilation_errors() {
        val compilation = WebpackCompiler().compile(pageOptions("syntaxError.js"))

        compilation shouldHaveError "Module build failed: SyntaxError: Unexpected token"
    }
}
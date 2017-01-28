package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.extensions.path.toPath
import geowarin.bootwebpack.utils.pageOptions
import geowarin.bootwebpack.utils.shouldContainAssets
import geowarin.bootwebpack.utils.shouldHaveError
import geowarin.bootwebpack.utils.source
import org.amshove.kluent.shouldEqual
import org.junit.Test

class WebpackCompilerTest {

    @Test
    fun dll_generation() {
        val compilation = DefaultWebpackCompiler().generateDll(
                // FIXME
                pageOptions("/Users/geowarin/dev/mgp/myprivateadvisor/src/main/js".toPath())
        )

        compilation shouldContainAssets listOf("vendors.dll.js", "vendors.manifest.json")
    }

    @Test
    fun compilation_succeeds() {
        val compilation = DefaultWebpackCompiler().compile(pageOptions("home.js"))

        compilation shouldContainAssets listOf("client.js", "common.js", "home.js", "renderer.js")
        assert(compilation.compileTime > 0, { -> "Should have a compile time" })
    }

    @Test
    fun compilation_postcss() {
        val compilation = DefaultWebpackCompiler().compile(pageOptions("css/styled.js"))

        val source = compilation.source { it.name.endsWith("css") }
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
    fun compilation_fonts() {
        val compilation = DefaultWebpackCompiler().compile(pageOptions("fonts/styled.js"))

        val source = compilation.source { it.name.endsWith("css") }
        source shouldEqual """@font-face {
  src: url(fonts/fontello-webfont32194e02049a53df82b262c7294c6a3b.woff2) format('woff2');
}
"""
        compilation shouldContainAssets listOf("client.js", "common.js", "fonts/fontello-webfont32194e02049a53df82b262c7294c6a3b.woff2", "renderer.js", "styled.aced6c0a.css", "styled.js")
    }


    @Test
    fun compilation_errors() {
        val compilation = DefaultWebpackCompiler().compile(pageOptions("syntaxError.js"))

        compilation shouldHaveError """ERROR in ./build/resources/test/syntaxError.js:
SyntaxError: Unexpected token (6:19)

  4 |${" "}
  5 |     render() {
> 6 |         return <div</div>;
    |                    ^
  7 |     }
  8 | }

"""
    }
}
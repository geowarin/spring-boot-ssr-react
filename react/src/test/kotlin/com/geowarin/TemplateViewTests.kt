package com.geowarin

import geowarin.bootwebpack.v8.V8ScriptTemplateView
import geowarin.bootwebpack.webpack.AssetStore
import geowarin.bootwebpack.webpack.CompilationError
import geowarin.bootwebpack.webpack.CompilationSuccess
import geowarin.bootwebpack.webpack.WebpackCompiler
import org.amshove.kluent.shouldEqual
import org.jsoup.Jsoup
import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.io.File

//@RunWith(SpringRunner::class)
//@SpringBootTest(classes = arrayOf(Toto::class))
class TemplateViewTests {

//    @Autowired
//    lateinit var v8view: V8ScriptTemplateView

    @Test
    fun should_render() {
        val assetStore = compile()

        val v8view = V8ScriptTemplateView(assetStore)
        v8view.url = "home.js"

        val request = MockHttpServletRequest("GET", "home.js")
        val response = MockHttpServletResponse()
        val model: Map<String, *> = mapOf(
                Pair("message", "Hello, world!")
        )

        v8view.render(model, request, response)

        val document = Jsoup.parse(response.contentAsString)
        val renderedHtmlText = document.body().select("#app").text()
        renderedHtmlText shouldEqual "Hello, world!"
    }
}

fun compile(): AssetStore {
    val compiler = WebpackCompiler(
            userProject = File("/Users/geowarin/dev/projects/boot-wp/demo/src/main/js"),
            bootSsrDirectory = File("/Users/geowarin/dev/projects/boot-wp/react/boot-ssr/")
    )

    val compilation = compiler.compile()
    when (compilation) {
        is CompilationSuccess -> {
            val assetStore = AssetStore()
            assetStore.store(compilation.assets)
            return assetStore
        }
        is CompilationError -> throw AssertionError("Could not compile: ${compilation.errorMessages}")
        else -> throw AssertionError("Could not compile")
    }
}

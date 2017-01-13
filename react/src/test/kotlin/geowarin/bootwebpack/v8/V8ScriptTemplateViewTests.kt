package geowarin.bootwebpack.v8

import geowarin.bootwebpack.utils.createTestCompiler
import geowarin.bootwebpack.utils.pageOptions
import geowarin.bootwebpack.v8.V8ScriptTemplateView
import geowarin.bootwebpack.webpack.AssetStore
import org.amshove.kluent.shouldEqual
import org.jsoup.Jsoup
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class V8ScriptTemplateViewTests {

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
        response.contentType shouldEqual MediaType.TEXT_HTML_VALUE
    }
}

fun compile(): AssetStore {

    val compiler = createTestCompiler()

    val compilation = compiler.compile(pageOptions("home.js"))
    if (compilation.hasErrors()) {
        throw AssertionError("Could not compile: ${compilation.errors}")
    }
    val assetStore = AssetStore()
    assetStore.store(compilation.assets)
    return assetStore
}

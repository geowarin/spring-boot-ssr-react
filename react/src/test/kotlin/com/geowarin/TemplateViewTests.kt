package com.geowarin

import com.geowarin.utils.createTestCompiler
import geowarin.bootwebpack.v8.V8ScriptTemplateView
import geowarin.bootwebpack.webpack.AssetStore
import org.amshove.kluent.shouldEqual
import org.jsoup.Jsoup
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class TemplateViewTests {

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

    val compiler = createTestCompiler("home.js")

    val compilation = compiler.compile()
    if (compilation.hasErrors()) {
        throw AssertionError("Could not compile: ${compilation.errors}")
    }
    val assetStore = AssetStore()
    assetStore.store(compilation.assets)
    return assetStore
}

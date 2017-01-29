package geowarin.bootwebpack.v8

import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8ScriptException
import com.fasterxml.jackson.databind.ObjectMapper
import geowarin.bootwebpack.template.HtmlTemplate
import geowarin.bootwebpack.webpack.AssetStore
import mu.KotlinLogging
import org.springframework.beans.factory.BeanFactoryUtils.beanOfTypeIncludingAncestors
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.util.StringUtils
import org.springframework.web.servlet.view.AbstractUrlBasedView
import java.nio.charset.Charset
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class V8ScriptTemplateView() : AbstractUrlBasedView() {
    private val log = KotlinLogging.logger {}
    private val CHARSET = Charset.forName("UTF-8")
    private var assetStore: AssetStore? = null

    constructor(assetStore: AssetStore) : this() {
        this.assetStore = assetStore
    }

    override fun prepareResponse(request: HttpServletRequest, response: HttpServletResponse) {
        super.prepareResponse(request, response)

        setResponseContentType(request, response)
        response.characterEncoding = CHARSET.name()
        response.contentType = MediaType.TEXT_HTML_VALUE
    }

    override fun renderMergedOutputModel(model: Map<String, Any>, request: HttpServletRequest, response: HttpServletResponse) {

        if (isJsonContentType(request)) {
            // TODO: test js routing
            val modelAndScript = ModelAndScript(model, getCurrentChunkName())
            response.writer.write(ObjectMapper().writeValueAsString(modelAndScript))
            return
        }

        val v8Script = V8Script(getAssetStore())
        try {
            // https://github.com/facebook/react/issues/6451
            v8Script.execute(ClassPathResource("object.assign.polyfill.js"))
            if (getAssetStore().hasChunk("vendors.dll.js")) {
                v8Script.execute("vendors.dll.js")
            }
            v8Script.execute("common.js")
            val rendererFun = v8Script.executeAndGet("renderer.js") as V8Function
            val component = v8Script.executeAndGet(url) as V8Function
            val renderedHtml = v8Script.executeFunction(rendererFun, component, model, url) as String

            val componentPropsJson = ObjectMapper().writeValueAsString(model)

            val cssUrls = getAssetStore().getCssNames()
            response.writer.write(indexTemplate(componentPropsJson, renderedHtml, cssUrls))

        } catch (e: V8ScriptException) {

            log.error { "Error while rendering the page $url:\n ${e.message}\n ${e.jsStackTrace}" }
            val errorTemplate = error(e)
            response.writer.write(errorTemplate)

        } finally {
            v8Script.release()
        }
    }

    private fun getCurrentChunkName() = getAssetStore().ensureAssetByChunkName(url).name

    private fun error(e: V8ScriptException): String {
        val errorTemplate =
                HtmlTemplate.fromResource(ClassPathResource("templates/error.html"))
                        .template(
                                "message" to (e.message ?: ""),
                                "jsStack" to e.jsStackTrace,
                                "path" to getCurrentChunkName()
                        )
                        .toString()
        return errorTemplate
    }

    private fun indexTemplate(componentPropsJson: String, renderedHtml: String, cssUrls: List<String>): String {
        val builder = HtmlTemplate.fromResource(ClassPathResource("templates/index.html"))
        cssUrls.forEach { url ->
            builder.insertCssTag(url)
        }
        if (getAssetStore().hasChunk("vendors.dll.js")) {
            builder.insertScriptTag("vendors.dll.js")
        }

        val currentChunkName = getCurrentChunkName()
        builder.insertChunk("common.js")
                .insertScriptTag("$currentChunkName?modulePath=window.currentComponent")
                .insertScript("window.currentProps = $componentPropsJson;")
                .insertChunk("client.js")
                .replaceNodeContent("#app", renderedHtml)

        val finalHtml = builder.toString()
        return finalHtml
    }

    fun HtmlTemplate.insertChunk(chunkName:String):HtmlTemplate {
        val chunk = getAssetStore().ensureAssetByChunkName(chunkName).name
        this.insertScriptTag(chunk)
        return this
    }

    override fun checkResource(locale: Locale?): Boolean {
        return getAssetStore().hasChunk(url)
    }

    private fun isJsonContentType(request: HttpServletRequest) =
            StringUtils.hasText(request.contentType)
                    && MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(request.contentType))

    private fun getAssetStore(): AssetStore {
        if (assetStore == null) {
            this.assetStore = beanOfTypeIncludingAncestors(applicationContext, AssetStore::class.java)
        }
        return this.assetStore!!
    }
}

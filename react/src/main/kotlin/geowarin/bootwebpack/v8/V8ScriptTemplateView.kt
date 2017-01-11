package geowarin.bootwebpack.v8

import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8ScriptException
import com.fasterxml.jackson.databind.ObjectMapper
import geowarin.bootwebpack.template.SimpleTemplate
import geowarin.bootwebpack.webpack.AssetStore
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
            val modelAndScript = ModelAndScript(model, url)
            response.writer.write(ObjectMapper().writeValueAsString(modelAndScript))
            return
        }

        val v8Script = V8Script(getAssetStore())
        try {
            v8Script.execute("common.js")
            val rendererFun = v8Script.executeAndGet("renderer.js") as V8Function
            val component = v8Script.executeAndGet(url) as V8Function
            val renderedHtml = v8Script.executeFunction(rendererFun, component, model, url) as String

            val componentPropsJson = ObjectMapper().writeValueAsString(model)

            response.writer.write(indexTemplate(componentPropsJson, renderedHtml))

        } catch (e: V8ScriptException) {

            System.err.println(e)

            val errorTemplate =
                    SimpleTemplate.fromResource(ClassPathResource("templates/error.html"))
                            .template(
                                    "message" to (e.message ?: ""),
                                    "jsStack" to e.jsStackTrace
                            )
                            .toString()

            response.writer.write(errorTemplate)

        } finally {
            v8Script.release()
        }
    }

    private fun indexTemplate(componentPropsJson: String, renderedHtml: String): String {
        val finalHtml = SimpleTemplate.fromResource(ClassPathResource("templates/index.html"))
                .insertScriptTag("common.js")
                .insertScriptTag("$url?modulePath=window.currentComponent")
                .insertScript("window.currentProps = $componentPropsJson;")
                .insertScriptTag("client.js")
                .replaceNodeContent("#app", renderedHtml)
                .toString()
        return finalHtml
    }

    override fun checkResource(locale: Locale?): Boolean {
        return getAssetStore().hasAsset(url)
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

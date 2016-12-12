package geowarin.bootwebpack.v8

import com.eclipsesource.v8.V8Function
import com.fasterxml.jackson.databind.ObjectMapper
import geowarin.bootwebpack.webpack.AssetStore
import org.springframework.beans.factory.BeanFactoryUtils.beanOfTypeIncludingAncestors
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ResourceLoader
import org.springframework.http.MediaType
import org.springframework.util.StringUtils
import org.springframework.web.servlet.view.AbstractUrlBasedView
import java.nio.charset.Charset
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class V8ScriptTemplateView() : AbstractUrlBasedView() {
    private var resourceLoader: ResourceLoader? = null

    override fun setContentType(contentType: String) {
        super.setContentType(contentType)
    }

    override fun prepareResponse(request: HttpServletRequest, response: HttpServletResponse) {
        super.prepareResponse(request, response)

        setResponseContentType(request, response)
        response.characterEncoding = CHARSET.name()
    }

    private val html: String = """
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <title>Webpack App</title>
  </head>
  <body>
  <div id="app">{renderedHtml}</div>
  <script type="text/javascript" src="common.js"></script>
  <script type="text/javascript" src="{componentPath}?modulePath=window.currentComponent"></script>
  <script type="text/javascript">
    window.currentProps = {componentProps};
  </script>
  <script type="text/javascript" src="client.js"></script>
  </body>
</html>
"""

    override fun renderMergedOutputModel(model: Map<String, Any>, request: HttpServletRequest, response: HttpServletResponse) {

        if (isJsonContentType(request)) {
            val modelAndScript = ModelAndScript(model, url)
            response.writer.write(ObjectMapper().writeValueAsString(modelAndScript))
            return
        }

        val v8Script = V8Script(getAssetStore())
        v8Script.execute("common.js")
        v8Script.execute("vendors.js")
        val rendererFun = v8Script.executeAndGet("renderer.js") as V8Function
        val component = v8Script.executeAndGet(url) as V8Function

        val renderedHtml = v8Script.executeFunction(rendererFun, component, model, url) as String

        val componentPropsJson = ObjectMapper().writeValueAsString(model)
        val finalHtml = html
                .replace("{componentPath}", url)
                .replace("{componentProps}", componentPropsJson)
                .replace("{renderedHtml}", renderedHtml)
        response.writer.write(finalHtml)

        v8Script.release()
    }

    private fun isJsonContentType(request: HttpServletRequest) =
            StringUtils.hasText(request.contentType)
                    && MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(request.contentType))

//    private fun getResource(location: String): Resource {
//        val resource = this.resourceLoader!!.getResource(location)
//        if (!resource.exists()) {
//            throw IllegalStateException(String.format("Resource %s not found", location))
//        }
//
//        return resource
//    }
//
//    @Throws(IOException::class)
//    private fun getResourceAsString(path: String): String {
//        val resource = getResource(path)
//        val reader = InputStreamReader(resource.inputStream, CHARSET)
//        return FileCopyUtils.copyToString(reader)
//    }

    override fun initApplicationContext(context: ApplicationContext) {
        super.initApplicationContext(context)

        if (this.resourceLoader == null) {
            this.resourceLoader = applicationContext
        }
    }

    private fun getAssetStore(): AssetStore {
        return beanOfTypeIncludingAncestors(applicationContext, AssetStore::class.java)
    }

    companion object {
        private val CHARSET = Charset.forName("UTF-8")
    }
}

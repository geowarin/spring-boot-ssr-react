package geowarin.bootwebpack.template

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.core.io.Resource
import org.springframework.web.util.HtmlUtils

val defaultHtml: String = """
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
  </head>
  <body>
  </body>
</html>
"""

class SimpleTemplate(html: String = defaultHtml) {
    internal var document: Document

    init {
        document = parse(html)
    }

    companion object Factory {
        fun fromResource(resource: Resource): SimpleTemplate {
            return SimpleTemplate(resource.file.readText())
        }
    }

    fun setTitle(title: String): SimpleTemplate {
        document.title(title)
        return this
    }

    fun insertScriptTag(scriptUrl: String): SimpleTemplate {
        document.body().appendElement("script").attr("type", "text/javascript").attr("src", scriptUrl)
        return this
    }

    fun insertScript(scriptText: String): SimpleTemplate {
        document.body().appendElement("script").attr("type", "text/javascript").text(scriptText)
        return this
    }

    fun replaceNodeContent(selector:String, content:String): SimpleTemplate {
        document.select(selector).html(content)
        return this
    }

    fun template(vararg values: Pair<String, String>): SimpleTemplate {
        var html = document.html()
        values.forEach {
            html = html.replace("{${it.first}}", HtmlUtils.htmlEscape(it.second))
        }
        document = parse(html)

        return this
    }

    override fun toString(): String {
        return document.toString()
    }

    private fun parse(html: String): Document {
        val document = Jsoup.parse(html)
        val outputSettings = Document.OutputSettings()
        // React does not like text nodes around rendered html...
        outputSettings.prettyPrint(false)
        document.outputSettings(outputSettings)
        return document
    }
}

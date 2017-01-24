package geowarin.bootwebpack.template

import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import org.junit.Test

class HtmlTemplateTest {

    @Test
    fun shouldInsertScriptTag() {
        val simpleTemplate = HtmlTemplate()
        simpleTemplate.insertScriptTag("myScript.js")

        val html = simpleTemplate.toString()
        html shouldContain """<script type="text/javascript" src="myScript.js"></script>"""
    }

    @Test
    fun shouldInsertScript() {
        val simpleTemplate = HtmlTemplate()
        simpleTemplate.insertScript("console.log('hello')")

        val html = simpleTemplate.toString()
        html shouldContain """<script type="text/javascript">console.log('hello')</script>"""
    }

    @Test
    fun shouldChangeTitle() {
        val simpleTemplate = HtmlTemplate()
        simpleTemplate.setTitle("Hello world")

        val html = simpleTemplate.toString()
        html shouldContain """<title>Hello world</title>"""
    }

    @Test
    fun canBeInitializedWithText() {
        val simpleTemplate = HtmlTemplate("""<html><div id="app"></div></html>""")

        val html = simpleTemplate.toString()
        html shouldEqual """<html><head></head><body><div id="app"></div></body></html>"""
    }

    @Test
    fun shouldBeTemplatable() {
        val simpleTemplate = HtmlTemplate("""<html><div id="app">{html}</div></html>""")
        simpleTemplate.template("html" to "Hello")

        val html = simpleTemplate.toString()
        html shouldEqual """<html><head></head><body><div id="app">Hello</div></body></html>"""
    }

    @Test
    fun shouldAcceptNullValueAsTemplat() {
        val simpleTemplate = HtmlTemplate("""<html><div id="app">{html}</div></html>""")
        simpleTemplate.template("html" to null)

        val html = simpleTemplate.toString()
        html shouldEqual """<html><head></head><body><div id="app"></div></body></html>"""
    }

    @Test
    fun shouldInsertInElement() {
        val simpleTemplate = HtmlTemplate("""<html><div id="app">{html}</div></html>""")
        simpleTemplate.replaceNodeContent("#app", "<div>Hello</div>")

        val html = simpleTemplate.toString()
        html shouldEqual """<html><head></head><body><div id="app"><div>Hello</div></div></body></html>"""
    }
}

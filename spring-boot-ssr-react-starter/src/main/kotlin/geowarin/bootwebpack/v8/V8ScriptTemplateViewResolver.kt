package geowarin.bootwebpack.v8

import org.springframework.web.servlet.view.UrlBasedViewResolver

class V8ScriptTemplateViewResolver() : UrlBasedViewResolver() {
    init {
        viewClass = requiredViewClass()
    }

    constructor(prefix: String, suffix: String, useCache: Boolean) : this() {
        setPrefix(prefix)
        setSuffix(suffix)
        isCache = useCache
    }

    override fun requiredViewClass(): Class<*> {
        return V8ScriptTemplateView::class.java
    }
}

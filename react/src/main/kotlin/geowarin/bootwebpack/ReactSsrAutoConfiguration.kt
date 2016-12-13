package geowarin.bootwebpack

import geowarin.bootwebpack.v8.V8ScriptTemplateViewResolver
import geowarin.bootwebpack.webpack.AssetStore
import geowarin.bootwebpack.webpack.WebpackResourceResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.ResourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@Configuration
open class ReactSsrAutoConfiguration : WebMvcConfigurerAdapter() {

    @Autowired
    lateinit var resourceProperties: ResourceProperties

    @Bean
    open fun viewResolver(): ViewResolver {
        val v8ScriptTemplateViewResolver = V8ScriptTemplateViewResolver("", ".js")
        v8ScriptTemplateViewResolver.order = Ordered.HIGHEST_PRECEDENCE
        return v8ScriptTemplateViewResolver
    }

    @Bean
    open fun assetStore(): AssetStore {
        return AssetStore()
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(*resourceProperties.staticLocations)
                .resourceChain(resourceProperties.chain.isCache)
                .addResolver(WebpackResourceResolver(assetStore()))
    }
}


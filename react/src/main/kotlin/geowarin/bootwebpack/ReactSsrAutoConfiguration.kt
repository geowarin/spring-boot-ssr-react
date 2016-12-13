package geowarin.bootwebpack

import geowarin.bootwebpack.v8.V8ScriptTemplateViewResolver
import geowarin.bootwebpack.webpack.AssetStore
import geowarin.bootwebpack.webpack.WebpackConnection
import geowarin.bootwebpack.webpack.WebpackResourceResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@Configuration
open class ReactSsrAutoConfiguration : WebMvcConfigurerAdapter() {

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

    @Bean
    open fun webpackConnection(): WebpackConnection = WebpackConnection(assetStore())

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false)
                .addResolver(WebpackResourceResolver(assetStore()))
    }
}


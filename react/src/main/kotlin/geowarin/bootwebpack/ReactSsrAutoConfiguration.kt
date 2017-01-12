package geowarin.bootwebpack

import geowarin.bootwebpack.config.ReactSsrProperties
import geowarin.bootwebpack.v8.V8ScriptTemplateViewResolver
import geowarin.bootwebpack.webpack.AssetStore
import geowarin.bootwebpack.webpack.WebpackResourceResolver
import geowarin.bootwebpack.webpack.WebpackWatcher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.ResourceProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@Configuration
@EnableConfigurationProperties(ReactSsrProperties::class)
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

    @Bean
    open fun webpackWatcher(properties: ReactSsrProperties): WebpackWatcher {
        return WebpackWatcher(assetStore(), properties)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(*resourceProperties.staticLocations)
                .resourceChain(resourceProperties.chain.isCache)
                .addResolver(WebpackResourceResolver(assetStore()))
    }
}


package geowarin.bootwebpack

import geowarin.bootwebpack.conditions.ConditionalOnDevelopmentMode
import geowarin.bootwebpack.conditions.ConditionalOnProductionMode
import geowarin.bootwebpack.config.BootSsrConfigurationFactory
import geowarin.bootwebpack.config.ReactSsrProperties
import geowarin.bootwebpack.config.RunMode
import geowarin.bootwebpack.v8.V8ScriptTemplateViewResolver
import geowarin.bootwebpack.webpack.AssetStore
import geowarin.bootwebpack.webpack.WebpackAssetLoader
import geowarin.bootwebpack.webpack.WebpackResourceResolver
import geowarin.bootwebpack.webpack.WebpackWatcher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.boot.autoconfigure.web.ResourceProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.devtools.autoconfigure.OptionalLiveReloadServer
import org.springframework.boot.devtools.restart.RestartScope
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
    open fun viewResolver(properties: ReactSsrProperties): ViewResolver {
        // FIXME: handle 'auto'
        val useCache = properties.mode == RunMode.production
        val v8ScriptTemplateViewResolver = V8ScriptTemplateViewResolver("", ".js", useCache)
        v8ScriptTemplateViewResolver.order = Ordered.HIGHEST_PRECEDENCE
        return v8ScriptTemplateViewResolver
    }

    @Bean
    open fun assetStore(): AssetStore {
        return AssetStore()
    }

    @Bean
    @ConditionalOnProductionMode
    open fun staticAssetStoreFeeder(assetStore: AssetStore, properties: ReactSsrProperties) = CommandLineRunner {
        WebpackAssetLoader().loadAssetsFromLocation(assetStore, properties.webpackAssetsLocation)
    }

    @Bean
    open fun configurationFactory(properties: ReactSsrProperties): BootSsrConfigurationFactory {
        return BootSsrConfigurationFactory(properties)
    }

    @Configuration
    @ConditionalOnClass(OptionalLiveReloadServer::class)
    open class WithLivereload {

        @Bean
        @ConditionalOnDevelopmentMode
        @RestartScope
        open fun webpackWatcher(assetStore: AssetStore, configurationFactory: BootSsrConfigurationFactory, liveReloadServer: OptionalLiveReloadServer): WebpackWatcher {
            return WebpackWatcher(assetStore, configurationFactory, { -> liveReloadServer.triggerReload() })
        }
    }

    @Configuration
    @ConditionalOnMissingClass("org.springframework.boot.devtools.autoconfigure.OptionalLiveReloadServer")
    open class WithoutLivereload {

        @Bean
        @ConditionalOnDevelopmentMode
        @RestartScope
        open fun webpackWatcher(assetStore: AssetStore, configurationFactory: BootSsrConfigurationFactory): WebpackWatcher {
            return WebpackWatcher(assetStore, configurationFactory)
        }
    }


    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(*resourceProperties.staticLocations)
                .setCachePeriod(resourceProperties.cachePeriod)
                .resourceChain(resourceProperties.chain.isCache)
                .addResolver(WebpackResourceResolver(assetStore()))
    }
}


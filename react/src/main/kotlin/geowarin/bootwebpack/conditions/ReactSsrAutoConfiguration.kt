package geowarin.bootwebpack.conditions

import geowarin.bootwebpack.config.RunMode
import org.springframework.boot.autoconfigure.condition.ConditionMessage
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.autoconfigure.condition.SpringBootCondition
import org.springframework.boot.bind.RelaxedPropertyResolver
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.ConfigurationCondition
import org.springframework.core.io.ClassPathResource
import org.springframework.core.type.AnnotatedTypeMetadata

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(OnProdCondition::class)
annotation class ConditionalOnDevelopmentMode

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(OnProdCondition::class)
annotation class ConditionalOnProductionMode

class OnProdCondition : SpringBootCondition(), ConfigurationCondition {

    val requiredResources = listOf(
            "renderer.js",
            "client.js"
    )

    override fun getConfigurationPhase(): ConfigurationCondition.ConfigurationPhase {
        return ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN
    }

    override fun getMatchOutcome(context: ConditionContext, metadata: AnnotatedTypeMetadata): ConditionOutcome {
        val environment = context.environment
        val resolver = RelaxedPropertyResolver(environment, "react.")
        val mode = resolver.getProperty("mode", RunMode::class.java, RunMode.auto)

        val assetsLocation = resolver.getProperty("webpackAssetsLocation", "/webpack_assets")

        val message = ConditionMessage.forCondition("Is run in production mode")
        val requiresProd = metadata.isAnnotated(ConditionalOnProductionMode::class.java.name)

        if (mode == RunMode.development) {
            return ConditionOutcome(!requiresProd, message.found("property").items("react.mode=" + mode))
        }
        if (mode == RunMode.production) {
            return ConditionOutcome(requiresProd, message.found("property").items("react.mode=" + mode))
        }

        // try to autodetect
        val requiredResourceFound = requiredResources.map { ClassPathResource("/$assetsLocation/$it") }.all { it.exists() }
        if (requiredResourceFound) {
            return ConditionOutcome.match(message.found("resources").items(requiredResources))
        }
        return ConditionOutcome.noMatch(message.didNotFind("resources").items(requiredResources))
    }

}
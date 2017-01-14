package geowarin.bootwebpack

import geowarin.bootwebpack.config.ReactSsrProperties
import geowarin.bootwebpack.config.WebpackOptionFactory
import geowarin.bootwebpack.extensions.path.*
import geowarin.bootwebpack.webpack.WebpackCompiler
import mu.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.GenericApplicationContext

@Configuration
@EnableConfigurationProperties(ReactSsrProperties::class)
open class TestConfig {

    private val logger = KotlinLogging.logger {}

    @Bean
    open fun runner(properties: ReactSsrProperties): CommandLineRunner = CommandLineRunner { args ->
        if (args.isEmpty()) {
            throw IllegalArgumentException("Error")
        }

        val projectDir = args[0].toPath()
        logger.info { "Compiling $projectDir" }
        val webpackCompilerOptions = WebpackOptionFactory().create(projectDir, properties)
        val compilationResult = WebpackCompiler().compile(webpackCompilerOptions)

        val distDir = projectDir / "target/classes/dist"
        distDir.deleteRecursively()
        compilationResult.assets.forEach { asset ->
            val destination = distDir / (asset.name + ".js")
            logger.info { "Writing $destination" }
            destination.parent.createDirectories()
            destination.writeText(asset.source)
        }
    }
}

fun main(args: Array<String>) {
    val springApplication = SpringApplication(TestConfig::class.java)
    springApplication.setApplicationContextClass(GenericApplicationContext::class.java)
    springApplication.run(*args)
}
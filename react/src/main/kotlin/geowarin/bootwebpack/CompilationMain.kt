package geowarin.bootwebpack

import geowarin.bootwebpack.config.ReactSsrProperties
import geowarin.bootwebpack.config.WebpackOptionFactory
import geowarin.bootwebpack.extensions.path.createDirectories
import geowarin.bootwebpack.extensions.path.div
import geowarin.bootwebpack.extensions.path.toPath
import geowarin.bootwebpack.extensions.path.writeText
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
        if (args.size < 2) {
            throw IllegalArgumentException("""The webpack compiler main requires 2 arguments:
- the project base dir (${'$'}{project.basedir} when running from maven)
- the assets destination in your jar (${'$'}{project.build.outputDirectory} when running from maven)
""")
        }

        val projectDir = args[0].toPath()
        val distDir = args[1].toPath() / properties.webpackAssetsLocation

        logger.info { "Compiling webpack assets of '$projectDir' to $distDir" }
        val webpackCompilerOptions = WebpackOptionFactory().create(projectDir, properties)
        val compilationResult = WebpackCompiler().compile(webpackCompilerOptions)

        if (compilationResult.hasErrors()) {
            throw Error("Webpack build encountered errors: " + compilationResult.errors.first().toString())
        }

        compilationResult.warnings.forEach { warning ->
            logger.warn { "webpack: ${warning.message}" }
        }

        compilationResult.assets.forEach { asset ->
            val destination = distDir / asset.name
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
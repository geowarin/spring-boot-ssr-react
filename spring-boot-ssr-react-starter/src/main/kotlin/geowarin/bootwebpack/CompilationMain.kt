package geowarin.bootwebpack

import geowarin.bootwebpack.config.BootSsrConfigurationFactory
import geowarin.bootwebpack.config.ReactSsrProperties
import geowarin.bootwebpack.extensions.path.createDirectories
import geowarin.bootwebpack.extensions.path.div
import geowarin.bootwebpack.extensions.path.writeBytes
import geowarin.bootwebpack.webpack.CompilationResult
import geowarin.bootwebpack.webpack.DefaultWebpackCompiler
import geowarin.bootwebpack.webpack.WebpackCompiler
import geowarin.bootwebpack.webpack.errors.ErrorLogger
import mu.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.GenericApplicationContext
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

class WebpackCompilationWriter {
    private val logger = KotlinLogging.logger {}

    fun write(compilationResult: CompilationResult, distDir: Path) {
        if (compilationResult.hasErrors()) {
            ErrorLogger().displayErrors(compilationResult.errors)
            throw Error("Webpack build encountered errors")
        }

        compilationResult.warnings.forEach { warning ->
            logger.warn { "webpack: ${warning.message}" }
        }

        compilationResult.assets.forEach { asset ->
            val destination = distDir / asset.name
            logger.info { "Writing $destination" }
            destination.parent.createDirectories()
            destination.writeBytes(asset.source)
        }
    }

}

class AssetWriterRunner(
        val configurationFactory: BootSsrConfigurationFactory,
        val fileSystem: FileSystem = FileSystems.getDefault(),
        val webpackCompiler: WebpackCompiler = DefaultWebpackCompiler()
) : CommandLineRunner {
    private val logger = KotlinLogging.logger {}

    override fun run(vararg args: String) {
        if (args.size < 2) {
            throw IllegalArgumentException("""The webpack compiler main requires 2 arguments:
- the project base dir (${'$'}{project.basedir} when running from maven)
- the assets destination in your jar (${'$'}{project.build.outputDirectory} when running from maven)
""")
        }

        val projectDir = fileSystem.getPath(args[0])
        configurationFactory.initializeProjectDir(projectDir)

        val options = configurationFactory.create()
        val distDir = options.additionalBuildInfo.distDir(fileSystem.getPath(args[1]))

        logger.info { "Compiling webpack assets of '$projectDir' to $distDir" }
        val compilationResult = webpackCompiler.compile(options.webpackCompilerOptions)
        WebpackCompilationWriter().write(compilationResult, distDir)
    }
}

@Configuration
@EnableConfigurationProperties(ReactSsrProperties::class)
open class CompilationConfig {

    @Bean
    open fun runner(properties: ReactSsrProperties): CommandLineRunner {
        val configurationFactory = BootSsrConfigurationFactory(properties)
        return AssetWriterRunner(configurationFactory)
    }
}

fun main(args: Array<String>) {
    val springApplication = SpringApplication(CompilationConfig::class.java)
    springApplication.setApplicationContextClass(GenericApplicationContext::class.java)
    springApplication.run(*args)
}
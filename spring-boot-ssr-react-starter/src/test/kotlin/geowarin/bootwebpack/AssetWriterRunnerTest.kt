package geowarin.bootwebpack

import geowarin.bootwebpack.config.BootSsrConfigurationFactory
import geowarin.bootwebpack.config.ReactSsrProperties
import geowarin.bootwebpack.extensions.path.exists
import geowarin.bootwebpack.extensions.path.readText
import geowarin.bootwebpack.utils.FileSystemRule
import geowarin.bootwebpack.webpack.Asset
import geowarin.bootwebpack.webpack.CompilationResult
import geowarin.bootwebpack.webpack.WebpackCompiler
import geowarin.bootwebpack.webpack.WebpackCompilerOptions
import io.reactivex.Flowable
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.junit.Rule
import org.junit.Test

class AssetWriterRunnerTest {

    @Rule @JvmField val fs = FileSystemRule()

    @Test
    fun should_write_assets() {
        val assets = listOf(
                Asset(name= "myFile.js", source = "Some js".toByteArray())
        )
        val configurationFactory = BootSsrConfigurationFactory(
                properties = ReactSsrProperties(),
                shouldCheckPaths = false
        )
        val assetWriterRunner = AssetWriterRunner(
                configurationFactory = configurationFactory,
                fileSystem = fs.fileSystem!!,
                webpackCompiler = MockWebpackCompiler(assets = assets)
        )

        assetWriterRunner.run("myProjectDir", "writeDir")

        val result = fs.getPath("writeDir/webpack_assets/myFile.js")
        result.exists shouldBe true
        result.readText() shouldEqual "Some js"
    }
}

class MockWebpackCompiler(val assets: List<Asset>) : WebpackCompiler {
    override fun generateDll(options: WebpackCompilerOptions): CompilationResult {
        TODO("not implemented")
    }

    override fun compile(options: WebpackCompilerOptions): CompilationResult {
        return CompilationResult(
                errors = listOf(),
                warnings = listOf(),
                assets = assets,
                compileTime = 42
        )
    }

    override fun watchAsync(options: WebpackCompilerOptions): Flowable<CompilationResult> {
        TODO("not implemented")
    }

    override fun stop() {
        TODO("not implemented")
    }
}

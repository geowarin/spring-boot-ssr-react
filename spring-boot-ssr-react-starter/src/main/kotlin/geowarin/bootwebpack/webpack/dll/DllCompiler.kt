package geowarin.bootwebpack.webpack.dll

import com.fasterxml.jackson.databind.ObjectMapper
import geowarin.bootwebpack.WebpackCompilationWriter
import geowarin.bootwebpack.config.BootSsrConfiguration
import geowarin.bootwebpack.extensions.path.*
import geowarin.bootwebpack.webpack.Asset
import geowarin.bootwebpack.webpack.CompilationResult
import geowarin.bootwebpack.webpack.DefaultWebpackCompiler
import geowarin.bootwebpack.webpack.WebpackCompiler
import mu.KotlinLogging
import java.nio.file.Path
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

data class DllMetadata(val yarnChecksum: String = "")

data class DllAssets(val vendorsJs: Asset, val vendorsManifest: Asset)

class DllCompiler(val webpackCompiler: WebpackCompiler = DefaultWebpackCompiler()) {

    private val logger = KotlinLogging.logger {}

    fun generateDll(config: BootSsrConfiguration): DllAssets {

        val jsSourceDir = config.additionalBuildInfo.jsSourceDir
        val dllTempDir = jsSourceDir / ".react-ssr/dll"

        val currentMetadata = generateMetadata(jsSourceDir)
        val previousMetadata = loadPreviousMetadata(jsSourceDir)

        if (previousMetadata == currentMetadata) {

            logger.info { "Checksum from previous dll matches. Loading dll from $dllTempDir" }
            val dllAssets = loadDllFromTemp(jsSourceDir)
            if (dllAssets != null) {
                return dllAssets
            }
        }

        logger.info { "Generating DLL. This will take some time upfront but reduce compile and reload time" }
        val dllCompilation = webpackCompiler.generateDll(config.webpackCompilerOptions)

        if (dllCompilation.hasErrors()) {
            throw IllegalStateException("Error while generating the dll")
        }

        val vendors = dllCompilation.find("vendors.dll.js")

        val vendorsManifest = dllCompilation.find("vendors.manifest.json")
        logger.info { "Generated DLL in ${dllCompilation.compileTime}ms" }

        WebpackCompilationWriter().write(dllCompilation, dllTempDir)

        if (currentMetadata != null) {
            writeMetadata(jsSourceDir, currentMetadata)
        }

        return DllAssets(vendors, vendorsManifest)
    }

    private fun loadDllFromTemp(jsSourceDir: Path): DllAssets? {
        val dllTempDir = jsSourceDir / ".react-ssr/dll"

        val previousDll = dllTempDir / "vendors.dll.js"
        val previousManifest = dllTempDir / "vendors.manifest.json"

        if (previousDll.notExists || previousManifest.notExists) {
            return null
        }

        val vendorsJs = loadAsset(previousDll)
        val vendorsManifest = loadAsset(previousManifest)
        return DllAssets(vendorsJs, vendorsManifest)
    }

    fun loadAsset(assetFile: Path): Asset {
        return Asset(
                name = assetFile.fileName.toString(),
                source = assetFile.readBytes()
        )
    }

    fun generateMetadata(jsSourceDir: Path): DllMetadata? {
        val yarnLockFile = jsSourceDir / "yarn.lock"

        if (yarnLockFile.notExists) {
            logger.info { "No yarn.lock at $yarnLockFile, cannot generate checksum" }
            return null
        }
        val currentYarnChecksum = checksum(yarnLockFile)

        val dllMetadata = DllMetadata(currentYarnChecksum)
        return dllMetadata
    }

    fun loadPreviousMetadata(jsSourceDir: Path): DllMetadata? {
        val dllTempDir = jsSourceDir / ".react-ssr/dll"
        val metadataFile = dllTempDir / "dll.metadata.json"

        if (metadataFile.notExists) {
            logger.info { "No metadata file found at $metadataFile, could not preload dll" }
            return null
        }
        val metadataContents = metadataFile.readText()
        val dllMetadata = ObjectMapper().readValue(metadataContents, DllMetadata::class.java)
        return dllMetadata
    }

    fun writeMetadata(jsSourceDir: Path, dllMetadata: DllMetadata) {
        val dllTempDir = jsSourceDir / ".react-ssr/dll"
        val metadataFile = dllTempDir / "dll.metadata.json"

        if (metadataFile.notExists) {
            metadataFile.createFile()
        }
        val text = ObjectMapper().writeValueAsString(dllMetadata)
        metadataFile.writeText(text)
    }

    fun CompilationResult.find(name: String): Asset {
        return this.assets.find { it.name == name }
                ?: throw IllegalStateException("Could not find $name in the compilation")
    }

    fun checksum(file: Path): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(file.readBytes())
        return DatatypeConverter.printHexBinary(digest)
    }
}
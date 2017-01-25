package geowarin.bootwebpack.webpack.dll

import geowarin.bootwebpack.extensions.path.writeText
import geowarin.bootwebpack.utils.FileSystemRule
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotEqual
import org.junit.Rule
import org.junit.Test

class DllCompilerTest {

    @Rule @JvmField val fs = FileSystemRule()

    @Test
    fun write_and_load_metadata() {

        val jsSourceDir = fs.createPath("jsSourceDir")
        fs.createPath("jsSourceDir/.react-ssr/dll")

        val dllMetadata = DllMetadata("someChecksum")
        DllCompiler().writeMetadata(jsSourceDir, dllMetadata)
        val previousMetadata = DllCompiler().loadPreviousMetadata(jsSourceDir)

        dllMetadata shouldEqual previousMetadata
    }

    @Test
    fun checksum() {

        val file = fs.createPath("file.js")
        file.writeText("Hello")

        val checksum1 = DllCompiler().checksum(file)
        val checksum2 = DllCompiler().checksum(file)

        checksum1 shouldEqual checksum2

        file.writeText("World")
        val checksum3 = DllCompiler().checksum(file)

        checksum3 shouldNotEqual checksum1
    }
}
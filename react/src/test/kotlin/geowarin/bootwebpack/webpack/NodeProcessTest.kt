package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.utils.asFile
import org.junit.Test
import java.io.File

class NodeProcessTest {

    @Test
    fun shouldAddV8ConvertibleToRuntime() {
        val page = Page(File("/Users/geowarin"), "geowarin")
        val options = WebpackCompilerOptions(listOf(page))

        val script = """ console.assert(options.pages[0].name == 'geowarin') """.asFile()
        val nodeProcess = NodeProcess(script)

        nodeProcess.addObj("options", options)
        nodeProcess.startSync()
    }
}
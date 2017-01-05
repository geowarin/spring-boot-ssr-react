package geowarin.bootwebpack.webpack

import com.geowarin.utils.asFile
import org.junit.Test

class NodeProcessTest {

    @Test
    fun shouldAddStringToRuntime() {
        val script = """ console.log(thing) """.asFile()
        val nodeProcess = NodeProcess(script)

        nodeProcess.addObj("thing", "hey")
        nodeProcess.startSync()
    }

    @Test
    fun shouldAddArrayToRuntime() {
        val script = """ console.log(thing) """.asFile()
        val nodeProcess = NodeProcess(script)

        nodeProcess.addObj("thing", listOf("hello"))
        nodeProcess.startSync()
    }

    @Test
    fun shouldAddMapToRuntime() {
        val script = """ console.log(thing) """.asFile()
        val nodeProcess = NodeProcess(script)

        nodeProcess.addObj("thing", mapOf("key" to "value"))
        nodeProcess.startSync()
    }
}
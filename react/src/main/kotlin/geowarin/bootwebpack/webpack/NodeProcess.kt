package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.NodeJS
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import java.io.Closeable
import java.io.File
import kotlin.concurrent.thread

class NodeProcess(val scriptFile: File) : Closeable {
    var shouldRun = false
    var objects: MutableList<Pair<String, List<String>>> = mutableListOf()
    var methods: MutableList<Pair<String, (V8Array) -> Unit>> = mutableListOf()

    fun startAsync() {
        thread {
            startSync()
        }
    }

    fun startSync() {
        val nodeJS = NodeJS.createNodeJS()

        objects.forEach { obj ->
            val array = V8Array(nodeJS.runtime)
            for (element in obj.second) {
                array.push(element)
            }
            nodeJS.runtime.add(obj.first, array)
            array.release()
        }

        methods.forEach { entry ->
            nodeJS.runtime.registerJavaMethod({ _: V8Object, args: V8Array ->
                entry.second(args)
            }, entry.first)
        }

        shouldRun = true
        nodeJS.exec(scriptFile)
        while (nodeJS.isRunning && shouldRun) {
            nodeJS.handleMessage()
        }
        nodeJS.release()
    }

    fun stop() {
        shouldRun = false
    }

    fun registerJavaMethod(name: String, method: (V8Array) -> Unit) {
        methods.add(Pair(name, method))
    }

    fun addStringArray(name: String, source: List<String>) {
        objects.add(Pair(name, source))
    }

    override fun close() {
        stop()
    }
}
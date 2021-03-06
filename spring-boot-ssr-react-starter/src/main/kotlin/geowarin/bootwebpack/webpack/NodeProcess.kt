package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.NodeJS
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.utils.V8ObjectUtils
import geowarin.bootwebpack.v8.V8Convertible
import java.io.Closeable
import java.io.File
import kotlin.concurrent.thread

data class NamedObject(val name: String, val value: V8Convertible<*>)
data class NamedMethod(val name: String, val method: (V8Array) -> Unit)

class NodeProcess(val scriptFile: File) : Closeable {
    var shouldRun = true
    var objects: MutableList<NamedObject> = mutableListOf()
    var methods: MutableList<NamedMethod> = mutableListOf()
    var thread: Thread? = null

    fun startAsync() {
        thread = thread {
            start()
        }
    }

    fun start() {
        val nodeJS = NodeJS.createNodeJS()

        val runtime = nodeJS.runtime
        objects.forEach { obj ->
            addToRuntime(obj, runtime)
        }

        methods.forEach { entry ->
            runtime.registerJavaMethod({ _: V8Object, args: V8Array ->
                entry.method(args)
            }, entry.name)
        }

        nodeJS.exec(scriptFile)
        while (nodeJS.isRunning && shouldRun) {
            nodeJS.handleMessage()
        }
        nodeJS.release()
    }

    private fun addToRuntime(obj: NamedObject, runtime: V8) {
        val v8Object = V8ObjectUtils.toV8Object(runtime, obj.value.toMap())
        runtime.add(obj.name, v8Object)
        v8Object.release()
    }

    fun stop() {
        shouldRun = false
        thread?.join()
    }

    fun registerJavaMethod(name: String, method: (V8Array) -> Unit) {
        methods.add(NamedMethod(name, method))
    }

    fun addObj(name: String, source: V8Convertible<*>) {
        objects.add(NamedObject(name, source))
    }

    override fun close() {
        stop()
    }
}
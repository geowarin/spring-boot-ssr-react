package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.*
import com.eclipsesource.v8.utils.V8ObjectUtils
import java.io.Closeable
import java.io.File
import kotlin.concurrent.thread

data class NamedObject(val name:String, val value:Any)
data class NamedMethod(val name:String, val method:(V8Array) -> Unit)

class NodeProcess(val scriptFile: File) : Closeable {
    var shouldRun = false
    var objects: MutableList<NamedObject> = mutableListOf()
    var methods: MutableList<NamedMethod> = mutableListOf()

    fun startAsync() {
        thread {
            startSync()
        }
    }

    fun startSync() {
        val nodeJS = NodeJS.createNodeJS()

        val runtime = nodeJS.runtime
        objects.forEach { obj ->
            addToRuntime(obj, runtime)
        }

        methods.forEach { entry ->
            runtime.registerJavaMethod({ obj: V8Object, args: V8Array ->
                entry.method(args)
            }, entry.name)
        }

        shouldRun = true
        nodeJS.exec(scriptFile)
        while (nodeJS.isRunning && shouldRun) {
            nodeJS.handleMessage()
        }
        nodeJS.release()
    }

    private fun addToRuntime(obj: NamedObject, runtime: V8) {
        val v8thing = V8ObjectUtils.getV8Result(runtime, obj.value)
        when (v8thing) {
            is Int -> runtime.add(obj.name, v8thing)
            is Boolean -> runtime.add(obj.name, v8thing)
            is Double -> runtime.add(obj.name, v8thing)
            is String -> runtime.add(obj.name, v8thing)
            is V8Value -> {
                runtime.add(obj.name, v8thing)
                v8thing.release()
            }
            else -> throw IllegalArgumentException("Could not convert value")
        }
    }

    fun stop() {
        shouldRun = false
    }

    fun registerJavaMethod(name: String, method: (V8Array) -> Unit) {
        methods.add(NamedMethod(name, method))
    }

    fun addObj(name: String, source: Any) {
        objects.add(NamedObject(name, source))
    }

    override fun close() {
        stop()
    }
}
package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.NodeJS
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import rx.Emitter.BackpressureMode
import rx.Observable
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

typealias CompilationListener = (CompilationResult) -> Unit

class WebpackCompiler(var bootSsrDirectory: File, val pages: List<File>) {
    val listeners: Queue<CompilationListener> = ConcurrentLinkedQueue<CompilationListener>()
    var watching: Boolean = false

    fun compile(): CompilationResult {
        return watchAsync().toBlocking().first()
    }

    fun addCompilationListener(compilationListener: CompilationListener) {
        listeners.add(compilationListener)
    }

    private fun createNode(scriptPath: String): NodeJS {
        val nodeJS = NodeJS.createNodeJS()

        val runtime = nodeJS.runtime
        val pageArray = createPages(runtime)
        runtime.add("pages", pageArray)
        pageArray.release()

        runtime.registerJavaMethod({ _: V8Object, args: V8Array ->
            val errorObjs = args[0] as V8Object

            val error = Error.create(errorObjs)
            throw Exception(error.message)

        }, "errorCallback")

        runtime.registerJavaMethod({ _: V8Object, args: V8Array ->
            val errors = args[0] as V8Array
            val warnings = args[1] as V8Array
            val assets = args[2] as V8Array

            val compilation = CompilationResult.create(errors, warnings, assets)

            for (listener in listeners) {
                listener.invoke(compilation)
            }

        }, "compilationCallback")

        val nodeScript = File(bootSsrDirectory, scriptPath)
        nodeJS.exec(nodeScript)
        return nodeJS
    }

    fun watchAsync(): Observable<CompilationResult> {
        watching = true
        thread {
            val nodeJS = createNode("bin/watchEntry.js")
            while (nodeJS.isRunning && watching) {
                nodeJS.handleMessage()
            }
            nodeJS.release()
        }
        return Observable.fromEmitter<CompilationResult>({ emitter ->
            val listener: CompilationListener = { comp -> emitter.onNext(comp) }
            emitter.setCancellation { -> listeners.remove(listener) }
            addCompilationListener(listener)
        }, BackpressureMode.BUFFER)
    }

    private fun createPages(v8: V8): V8Array {
        val array = V8Array(v8)
        for (page in pages) {
            array.push(page.absolutePath)
        }
        return array
    }

    fun stopWatching() {
        watching = false
    }
}

data class Error(val message: String) {
    companion object Factory {
        fun create(exception: V8Object): Error = Error(exception.getString("message"))
    }
}

data class Warning(val message: String) {
    companion object Factory {
        fun create(exception: V8Object): Warning = Warning(exception.getString("message"))
    }
}

class CompilationResult(val errors: List<Error>, val warnings: List<Warning>, val assets: List<Asset>) {
    fun hasErrors() = errors.isNotEmpty()
    fun hasWarnings() = warnings.isNotEmpty()

    companion object Factory {
        fun create(errorsArray: V8Array, warningsArray: V8Array, assetsArray: V8Array): CompilationResult {

            val errors = toObjs(errorsArray)
            val warnings = toObjs(warningsArray)
            val assets = toObjs(assetsArray)


            val compilationResult = CompilationResult(
                    errors.map { Error.create(it) },
                    warnings.map { Warning.create(it) },
                    assets.map(::Asset)
            )

            errors.forEach { it.release() }
            warnings.forEach { it.release() }
            assets.forEach { it.release() }
            errorsArray.release()
            warningsArray.release()
            assetsArray.release()

            return compilationResult
        }
    }
}

fun toObjs(v8Array: V8Array): List<V8Object> = (0..v8Array.length() - 1).map { v8Array.getObject(it) }

package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import rx.Emitter
import rx.Emitter.BackpressureMode
import rx.Observable
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

typealias CompilationListener = (CompilationResult) -> Unit

class WebpackCompiler(var bootSsrDirectory: File, val pages: List<File>) {
    val listeners: Queue<CompilationListener> = ConcurrentLinkedQueue<CompilationListener>()
    lateinit var nodeProcess: NodeProcess

    fun compile(): CompilationResult {
        val watchScript = File(bootSsrDirectory, "bin/compileEntry.js")
        nodeProcess = createNodeProcess(watchScript)
        nodeProcess.startAsync()

        val observable = createObservable(BackpressureMode.DROP)
        return observable.toBlocking().first()
    }

    fun watchAsync(): Observable<CompilationResult> {
        val watchScript = File(bootSsrDirectory, "bin/watchEntry.js")
        nodeProcess = createNodeProcess(watchScript)
        nodeProcess.startAsync()

        return createObservable(BackpressureMode.BUFFER)
    }

    private fun createObservable(backpressureMode: Emitter.BackpressureMode): Observable<CompilationResult> {
        return Observable.fromEmitter<CompilationResult>({ emitter ->
            val listener: CompilationListener = { comp -> emitter.onNext(comp) }
            emitter.setCancellation { -> listeners.remove(listener) }
            listeners.add(listener)
        }, backpressureMode)
    }

    fun stop() {
        nodeProcess.stop()
    }

    private fun createNodeProcess(nodeScript: File): NodeProcess {
        val nodeProcess = NodeProcess(nodeScript)
        nodeProcess.addStringArray("pages", pages.map { it.absolutePath })

        nodeProcess.registerJavaMethod("errorCallback") { args ->
            val error = Error.create(exception = args[0] as V8Object)
            throw Exception(error.message)
        }

        nodeProcess.registerJavaMethod("compilationCallback") { args ->
            val compilation = CompilationResult.create(
                    errorsArray = args[0] as V8Array,
                    warningsArray = args[1] as V8Array,
                    assetsArray = args[2] as V8Array
            )

            for (listener in listeners) {
                listener.invoke(compilation)
            }
        }
        return nodeProcess
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

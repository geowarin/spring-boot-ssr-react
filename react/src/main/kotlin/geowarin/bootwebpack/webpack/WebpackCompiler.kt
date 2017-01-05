package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

//typealias CompilationListener = (CompilationResult) -> Unit

class WebpackCompiler(var bootSsrDirectory: File, val pages: Iterable<File>) {
    val listeners: Queue<(CompilationResult) -> Unit> = ConcurrentLinkedQueue()
    lateinit var nodeProcess: NodeProcess

    fun compile(): CompilationResult {
        val watchScript = File(bootSsrDirectory, "bin/compileEntry.js")
        val nodeProcess = createNodeProcess(watchScript)
        nodeProcess.startAsync()

        val observable = createObservable(BackpressureStrategy.DROP)
        return observable.blockingFirst()
    }

    fun watchAsync(vararg watchDirectories:File): Flowable<CompilationResult> {
        val watchScript = File(bootSsrDirectory, "bin/watchEntry.js")
        nodeProcess = createNodeProcess(watchScript, watchDirectories.toList())
        nodeProcess.startAsync()

        return createObservable(BackpressureStrategy.BUFFER)
    }

    private fun createObservable(backpressureStrategy: BackpressureStrategy): Flowable<CompilationResult> {
        return Flowable.create({ emitter: FlowableEmitter<CompilationResult> ->
            val listener: (CompilationResult) -> Unit = { comp -> emitter.onNext(comp) }
            emitter.setCancellable { -> listeners.remove(listener) }
            listeners.add(listener)
        }, backpressureStrategy)
    }

    fun stop() {
        nodeProcess.stop()
    }

    private fun createNodeProcess(nodeScript: File, watchDirectories:List<File> = listOf()): NodeProcess {
        val nodeProcess = NodeProcess(nodeScript)

        val options = mapOf<String, Any>(
                "pages" to pages.map { it.absolutePath },
                "watchDirectories" to watchDirectories.map { it.absolutePath }
        )
        nodeProcess.addObj("options", options)

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

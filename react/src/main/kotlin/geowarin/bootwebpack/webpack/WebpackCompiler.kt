package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import geowarin.bootwebpack.v8.V8Convertible
import geowarin.bootwebpack.v8.mappedBy
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

//typealias CompilationListener = (CompilationResult) -> Unit

data class Page(
        var file: File,
        val name: String
) : V8Convertible<Page>(
        { "file" mappedBy it.file.canonicalPath },
        { "name" mappedBy it.name }
)

data class WebpackCompilerOptions(
        val pages: List<Page>,
        val watchDirectories: List<String> = listOf()
) : V8Convertible<WebpackCompilerOptions>(
        { "pages" mappedBy it.pages },
        { "watchDirectories" mappedBy it.watchDirectories }
)

// TODO: put bootSsrDirectory in options
class WebpackCompiler(var bootSsrDirectory: File) {
    val listeners: Queue<(CompilationResult) -> Unit> = ConcurrentLinkedQueue()
    lateinit var nodeProcess: NodeProcess

    fun compile(options: WebpackCompilerOptions): CompilationResult {
        val watchScript = File(bootSsrDirectory, "bin/compileEntry.js")
        val nodeProcess = createNodeProcess(watchScript, options)
        nodeProcess.startAsync()

        val observable = createObservable(BackpressureStrategy.DROP)
        return observable.blockingFirst()
    }

    fun watchAsync(options: WebpackCompilerOptions): Flowable<CompilationResult> {
        val watchScript = File(bootSsrDirectory, "bin/watchEntry.js")
        nodeProcess = createNodeProcess(watchScript, options)
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

    private fun createNodeProcess(nodeScript: File, options: WebpackCompilerOptions): NodeProcess {
        val nodeProcess = NodeProcess(nodeScript)
        nodeProcess.addObj("options", options)

        nodeProcess.registerJavaMethod("errorCallback") { args ->
            val error = Error.create(exception = args[0] as V8Object)
            throw Exception(error.message)
        }

        nodeProcess.registerJavaMethod("compilationCallback") { args ->
            val compilation = CompilationResult.create(
                    errorsArray = args[0] as V8Array,
                    warningsArray = args[1] as V8Array,
                    assetsArray = args[2] as V8Array,
                    compileTime = args[3] as Int
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

class CompilationResult(val errors: List<Error>, val warnings: List<Warning>, val assets: List<Asset>, val compileTime: Int) {
    fun hasErrors() = errors.isNotEmpty()
    fun hasWarnings() = warnings.isNotEmpty()

    companion object Factory {
        fun create(errorsArray: V8Array, warningsArray: V8Array, assetsArray: V8Array, compileTime: Int): CompilationResult {

            val errors = toObjs(errorsArray)
            val warnings = toObjs(warningsArray)
            val assets = toObjs(assetsArray)

            val compilationResult = CompilationResult(
                    errors.map { Error.create(it) },
                    warnings.map { Warning.create(it) },
                    assets.map(::Asset),
                    compileTime
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

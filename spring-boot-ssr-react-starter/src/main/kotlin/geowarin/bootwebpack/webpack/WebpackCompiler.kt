package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import geowarin.bootwebpack.extensions.path.div
import geowarin.bootwebpack.v8.V8Convertible
import geowarin.bootwebpack.v8.mappedBy
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

typealias CompilationListener = (CompilationResult) -> Unit
typealias ErrorListener = (V8Error) -> Unit

data class WebpackListener(
        val compilationListener: CompilationListener,
        val errorListener: ErrorListener
)

data class Page(
        var path: Path,
        val name: String
) : V8Convertible<Page>(
        { "file" mappedBy it.path.toRealPath().toString() },
        { "name" mappedBy it.name }
)

data class WebpackCompilerOptions(
        val bootSsrDirectory: Path,
        val projectDirectory: Path,
        val pages: List<Page>,
        /**
         * FIXME: only in watch
         */
        val dllManifestContent: String? = null,
        /**
         * only in compile
         */
        val minify:Boolean = true,
        /**
         * only in compile
         */
        val generateStats:Boolean = false,
        /**
         * only in generateDll
         */
        val additionalDllLibs: List<String>? = listOf(),
        /**
         * only in watch
         */
        val watchDirectories: List<Path> = listOf()
) : V8Convertible<WebpackCompilerOptions>(
        { "pages" mappedBy it.pages },
        { "projectDirectory" mappedBy it.projectDirectory.toRealPath().toString() },
        { "watchDirectories" mappedBy it.watchDirectories.map { it.toRealPath().toString() } },
        { "dllManifestContent" mappedBy it.dllManifestContent },
        { "additionalDllLibs" mappedBy it.additionalDllLibs },
        { "minify" mappedBy it.minify },
        { "generateStats" mappedBy it.generateStats }
)

interface WebpackCompiler {
    fun generateDll(options: WebpackCompilerOptions): CompilationResult
    fun compile(options: WebpackCompilerOptions): CompilationResult
    fun watchAsync(options: WebpackCompilerOptions): Flowable<CompilationResult>
    fun stop()
}

class DefaultWebpackCompiler : WebpackCompiler {
    val listeners: Queue<WebpackListener> = ConcurrentLinkedQueue()
    lateinit var nodeProcess: NodeProcess

    override fun generateDll(options: WebpackCompilerOptions): CompilationResult {
        val watchScript = options.bootSsrDirectory / "bin/dllEntry.js"
        val nodeProcess = createNodeProcess(watchScript.toFile(), options)
        nodeProcess.startAsync()

        val observable = createObservable(BackpressureStrategy.DROP)
        val compilationResult = observable.blockingFirst()
        nodeProcess.stop()
        return compilationResult
    }

    override fun compile(options: WebpackCompilerOptions): CompilationResult {
        val watchScript = options.bootSsrDirectory / "bin/compileEntry.js"
        val nodeProcess = createNodeProcess(watchScript.toFile(), options)
        nodeProcess.startAsync()

        val observable = createObservable(BackpressureStrategy.DROP)
        val compilationResult = observable.blockingFirst()
        nodeProcess.stop()
        return compilationResult
    }

    override fun watchAsync(options: WebpackCompilerOptions): Flowable<CompilationResult> {
        val watchScript = options.bootSsrDirectory / "bin/watchEntry.js"
        nodeProcess = createNodeProcess(watchScript.toFile(), options)
        nodeProcess.startAsync()

        return createObservable(BackpressureStrategy.BUFFER)
    }

    fun createObservable(backpressureStrategy: BackpressureStrategy): Flowable<CompilationResult> {
        return Flowable.create({ emitter: FlowableEmitter<CompilationResult> ->
            val compilationListener = { comp: CompilationResult -> emitter.onNext(comp) }

            val errorListener = { error: V8Error ->
                val exception = Exception(error.toString())
                emitter.onError(exception)
            }
            val listener = WebpackListener(compilationListener, errorListener)

            emitter.setCancellable { -> listeners.remove(listener) }
            listeners.add(listener)
        }, backpressureStrategy)
    }

    override fun stop() {
        nodeProcess.stop()
    }

    private fun createNodeProcess(nodeScript: File, options: WebpackCompilerOptions): NodeProcess {
        val nodeProcess = NodeProcess(nodeScript)
        nodeProcess.addObj("options", options)

        nodeProcess.registerJavaMethod("errorCallback") { args ->
            val error = V8Error.create(exception = args[0] as V8Object)
            for (listener in listeners) {
                listener.errorListener.invoke(error)
            }
            // FIXME: this blocks forever
            nodeProcess.stop()
        }

        nodeProcess.registerJavaMethod("compilationCallback") { args ->
            val compilation = CompilationResult.create(
                    errorsArray = args[0] as V8Array,
                    warningsArray = args[1] as V8Array,
                    assetsArray = args[2] as V8Array,
                    compileTime = args[3] as Int
            )

            for (listener in listeners) {
                listener.compilationListener.invoke(compilation)
            }
        }
        return nodeProcess
    }
}

open class JsError(val message:String, val stack:String)

class WebpackError(message: String, stack: String, val file: String?, val severity: Int, val name: String): JsError(message, stack) {
    companion object Factory {
        fun create(exception: V8Object): WebpackError {
            val webpackError = exception.getObject("webpackError")
            val error = WebpackError(
                    message = exception.getString("message"),
                    file = exception.getString("file"),
                    name = exception.getString("name"),
                    severity = exception.getInteger("severity"),
                    stack = webpackError.getString("stack")
            )
            webpackError.release()
            exception.release()
            return error
        }
    }

    override fun toString(): String {
        return "$message in $file\n$stack"
    }
}

class V8Error(message: String, stack: String): JsError(message, stack) {
    companion object Factory {
        fun create(exception: V8Object): V8Error {
            val error = V8Error(
                    message = exception.getString("message"),
                    stack = exception.getString("stack")
            )
            exception.release()
            return error
        }
    }

    override fun toString(): String {
        return "$message\n$stack"
    }
}

data class Warning(val message: String) {
    companion object Factory {
        fun create(warningObj: V8Object): Warning {
            val warning = Warning(warningObj.getString("message"))
            warningObj.release()
            return warning
        }
    }

    override fun toString(): String {
        return message
    }
}

class CompilationResult(val errors: List<WebpackError>, val warnings: List<Warning>, val assets: List<Asset>, val compileTime: Int) {
    fun hasErrors() = errors.isNotEmpty()
    fun hasWarnings() = warnings.isNotEmpty()

    companion object Factory {
        fun create(errorsArray: V8Array, warningsArray: V8Array, assetsArray: V8Array, compileTime: Int): CompilationResult {

            val errors = toObjs(errorsArray)
            val warnings = toObjs(warningsArray)
            val assets = toObjs(assetsArray)

            val compilationResult = CompilationResult(
                    errors.map { WebpackError.create(it) },
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

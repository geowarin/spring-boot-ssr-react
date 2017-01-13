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

data class Page(
        var file: File,
        val name: String
) : V8Convertible<Page>(
        { "file" isA it.file.canonicalPath },
        { "name" isA it.name }
)

data class Options(
        val pages: List<Page>,
        val watchDirectories: List<String> = listOf()
) : V8Convertible<Options>(
        { "pages" isA it.pages },
        { "watchDirectories" isA it.watchDirectories }
)

infix fun <A, B : V8Convertible<*>> A.isA(that: B): Pair<A, Any> = Pair(this, that.toMap())
infix fun <A, B> A.isA(that: B): Pair<A, B> = Pair(this, that)
infix fun <A, B : List<V8Convertible<*>>> A.isA(that: B): Pair<A, Any> = Pair(this, that.map { it.toMap() })

abstract class V8Convertible<T>(vararg val props: (T) -> Pair<String, Any?>) {

    @Suppress("UNCHECKED_CAST")
    fun getThis(): T {
        return this as T
    }

    fun toMap(): Map<String, *> {
        return props.map { it.invoke(getThis()) }.toMap()
    }
}


class WebpackCompiler(var bootSsrDirectory: File) {
    val listeners: Queue<(CompilationResult) -> Unit> = ConcurrentLinkedQueue()
    lateinit var nodeProcess: NodeProcess

    fun compile(options: Options): CompilationResult {
        val watchScript = File(bootSsrDirectory, "bin/compileEntry.js")
        val nodeProcess = createNodeProcess(watchScript, options)
        nodeProcess.startAsync()

        val observable = createObservable(BackpressureStrategy.DROP)
        return observable.blockingFirst()
    }

    fun watchAsync(options: Options): Flowable<CompilationResult> {
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

    private fun createNodeProcess(nodeScript: File, options: Options): NodeProcess {
        val nodeProcess = NodeProcess(nodeScript)

//        val pagesV8 = pages.map { Page(it, it.name) }
//        val options = Options(
//                pages = pagesV8,
//                watchDirectories = watchDirectories.map { it.canonicalPath }
//        )
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

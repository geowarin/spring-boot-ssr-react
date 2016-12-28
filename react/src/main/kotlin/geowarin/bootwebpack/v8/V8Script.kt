package geowarin.bootwebpack.v8

import com.eclipsesource.v8.*
import com.eclipsesource.v8.utils.V8ObjectUtils
import geowarin.bootwebpack.webpack.AssetStore

interface Console {
    fun assert(vararg args: Any)
    fun dir(vararg args: Any)
    fun error(vararg messages: Any)
    fun info(vararg messages: Any)
    fun log(vararg messages: Any)
    fun time(label: String)
    fun timeEnd(label: String)
    fun trace(vararg messages: Any)
    fun warn(vararg messages: Any)
}

class StdoutConsole : Console {
    val times: MutableMap<String, Long> = mutableMapOf()
    val startTime = System.currentTimeMillis()

    override fun assert(vararg args: Any) {
        val value: Boolean = args[0] as Boolean
        val message = if (args.size > 1) args[1] as String else "Assertion Error"
        val remainingArgs = args.drop(2)

        kotlin.assert(value, { -> String.format(message, *remainingArgs.toTypedArray()) })
    }

    override fun dir(vararg args: Any) {
        val obj: Map<String, *> = V8ObjectUtils.toMap(args.first() as V8Object)
        println(obj.entries)
    }

    override fun error(vararg messages: Any) {
        System.err.println(messages.joinToString(" "))
    }

    override fun info(vararg messages: Any) {
        println(messages.joinToString(" "))
    }

    override fun log(vararg messages: Any) {
        println(messages.joinToString(" "))
    }

    override fun time(label: String) {
        times[label] = System.currentTimeMillis()
    }

    override fun timeEnd(label: String) {
        val watch = times.getOrDefault(label, startTime)
        println(String.format("%s: %dms", label, System.currentTimeMillis() - watch))
    }

    override fun trace(vararg messages: Any) {
        println(messages.joinToString(" "))
    }

    override fun warn(vararg messages: Any) {
        println(messages.joinToString(" "))
    }
}

class V8Script(val assetStore: AssetStore, console: Console = StdoutConsole()) {
    val toClean: MutableList<V8Value> = mutableListOf()
    val v8: V8 = V8.createV8Runtime("window")

    init {
        val v8Console = V8Object(v8)
        v8.add("console", v8Console)

        v8Console.registerJavaMethod(console, "assert", "assert", arrayOf<Class<*>>(Array<Any>::class.java))
        v8Console.registerJavaMethod(console, "dir", "dir", arrayOf<Class<*>>(Array<Any>::class.java))
        v8Console.registerJavaMethod(console, "error", "error", arrayOf<Class<*>>(Array<Any>::class.java))
        v8Console.registerJavaMethod(console, "info", "info", arrayOf<Class<*>>(Array<Any>::class.java))
        v8Console.registerJavaMethod(console, "log", "log", arrayOf<Class<*>>(Array<Any>::class.java))
        v8Console.registerJavaMethod(console, "time", "time", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "timeEnd", "timeEnd", arrayOf<Class<*>>(String::class.java))
        v8Console.registerJavaMethod(console, "trace", "trace", arrayOf<Class<*>>(Array<Any>::class.java))
        v8Console.registerJavaMethod(console, "warn", "warn", arrayOf<Class<*>>(Array<Any>::class.java))
        v8Console.release()
    }

    fun execute(path: String) {
        val scriptSrc = assetStore.getAssetSource(path) ?: throw IllegalStateException("Could not find script $path")
        val obj = v8.executeScript(scriptSrc)
        if (obj is V8Value) {
            toClean.add(obj)
        }
    }

    fun executeAndGet(path: String): V8Value {
        val scriptSrc = assetStore.getAssetSource(path) ?: throw IllegalStateException("Could not find script $path")
        val module = v8.executeScript(scriptSrc) as V8Object
        toClean.add(module)
        val default = module.get("default") as V8Function
        toClean.add(default)
        return default
    }

    fun executeFunction(function: V8Function, vararg params: Any): Any {
        val v8Params = V8Array(v8)

        for (param in params) {
            when (param) {
                is V8Value -> v8Params.push(param)
                is Map<*, *> -> {
                    val v8obj = V8ObjectUtils.toV8Object(v8, param as Map<String, *>)
                    v8Params.push(v8obj)
                    toClean.add(v8obj)
                }
                is String -> v8Params.push(param)
                is Int -> v8Params.push(param)
                is Double -> v8Params.push(param)
                is Boolean -> v8Params.push(param)
            }
        }
        toClean.add(v8Params)
        return function.call(null, v8Params)
    }

    fun release() {
        toClean.forEach { it.release() }
        v8.release()
    }
}
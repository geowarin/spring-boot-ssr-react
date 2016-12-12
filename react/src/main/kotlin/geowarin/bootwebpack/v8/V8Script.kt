package geowarin.bootwebpack.v8

import com.eclipsesource.v8.*
import com.eclipsesource.v8.utils.V8ObjectUtils
import geowarin.bootwebpack.webpack.AssetStore

class V8Script(val assetStore: AssetStore) {
    val toClean: MutableList<V8Value> = mutableListOf()
    val v8 = V8.createV8Runtime("window")

    fun execute(path: String) {
        val scriptSrc = assetStore.getAssetSource(path)
        val obj = v8.executeScript(scriptSrc)
        if (obj is V8Value) {
            toClean.add(obj)
        }
    }

    fun executeAndGet(path: String): V8Value {
        val scriptSrc = assetStore.getAssetSource(path)
        val module = v8.executeScript(scriptSrc) as V8Object
        toClean.add(module);
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
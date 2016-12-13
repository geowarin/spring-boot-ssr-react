package geowarin.bootwebpack.webpack

import com.eclipsesource.v8.JavaCallback
import com.eclipsesource.v8.NodeJS
import java.io.File

class WebpackCompiler(val userProject: File, var bootSsrDirectory: File) {

    val errors: MutableList<String> = mutableListOf()
    val assets: MutableList<Asset> = mutableListOf()

    fun assetCallback(name: String, source: String) {
        assets.add(Asset(name, source))
    }

    fun errorCallback(errorMessage: String) {
        errors.add(errorMessage)
    }

    fun compile(): Compilation {
        val nodeJS = NodeJS.createNodeJS()
        val getUserProjectPath = JavaCallback { v8Object, v8Array -> userProject.absolutePath }

        nodeJS.runtime.registerJavaMethod(getUserProjectPath, "getUserProjectPath")
        nodeJS.runtime.registerJavaMethod(this, "assetCallback", "assetCallback", arrayOf<Class<*>>(String::class.java, String::class.java))
        nodeJS.runtime.registerJavaMethod(this, "errorCallback", "errorCallback", arrayOf<Class<*>>(String::class.java))

        val nodeScript = File(bootSsrDirectory, "bin/compilerEntry.js")
        nodeJS.exec(nodeScript)

        while (nodeJS.isRunning) {
            nodeJS.handleMessage()
        }
        nodeJS.release()

        if (errors.isNotEmpty()) {
            return CompilationError(errors)
        }
        return CompilationSuccess(assets);
    }
}

open class Compilation
open class CompilationError(val errorMessages: List<String>) : Compilation()
open class CompilationSuccess(val assets: List<Asset>) : Compilation()

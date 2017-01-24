package geowarin.bootwebpack.config

import java.io.File
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.nio.file.Path

interface ProjectDirResolver {
    fun resolveProjectDirectory(mainApplicationClass: Class<*>): Path
}

class DefaultProjectDirResolver : ProjectDirResolver {
    private val defaultPath = File(System.getProperty("user.dir")).toPath()

    override fun resolveProjectDirectory(mainApplicationClass: Class<*>): Path {
        val source = findSource(mainApplicationClass)
        if (source == null) {
            return defaultPath
        }
        return getProjectDir(source.toPath())
    }

    private fun findSource(sourceClass: Class<*>?): File? {
        try {
            val domain = sourceClass?.protectionDomain
            val codeSource = domain?.codeSource
            val location = codeSource?.location
            val source = if (location == null) null else findSource(location)
            if (source != null && source.exists()) {
                return source.absoluteFile
            }
            return null
        } catch (ex: Exception) {
            return null
        }
    }

    @Throws(IOException::class)
    private fun findSource(location: URL): File? {
        val connection = location.openConnection()
        if (connection is JarURLConnection) {
            return null
        }
        return File(location.path)
    }

    fun getProjectDir(dir: Path): Path {
        if (dir.endsWith("target/classes")) {
            // maven
            return dir.parent.parent
        } else if (dir.endsWith("build/classes/main")) {
            // gradle
            return dir.parent.parent.parent
        } else {
            // fallback
            return defaultPath
        }
    }


}
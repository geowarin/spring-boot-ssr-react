package geowarin.bootwebpack.utils

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import geowarin.bootwebpack.extensions.path.delete
import geowarin.bootwebpack.extensions.path.fileExtension
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

class FileSystemRule(val configuration: Configuration = Configuration.forCurrentPlatform()) : TestRule {
    var fileSystem: FileSystem? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {

            override fun evaluate() {
                fileSystem = Jimfs.newFileSystem(configuration)
                try {
                    base.evaluate()
                } finally {
                    fileSystem!!.close()
                }
            }

        }
    }

    fun getPath(first: String, vararg more: String): Path {
        return fileSystem!!.getPath(first, *more)
    }

    fun delete(first: String, vararg more: String) {
        fileSystem!!.getPath(first, *more).delete()
    }

    fun createPath(first: String, vararg more: String): Path {
        val path = getPath(first, *more)
        if (path.fileExtension.isEmpty()) {
            Files.createDirectories(path)
        } else {
            Files.createDirectories(path.parent)
            Files.createFile(path)
        }
        return path
    }

}
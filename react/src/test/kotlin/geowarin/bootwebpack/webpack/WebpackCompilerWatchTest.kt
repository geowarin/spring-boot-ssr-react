package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.utils.createTestCompiler
import geowarin.bootwebpack.utils.pageOptions
import geowarin.bootwebpack.utils.toPages
import geowarin.bootwebpack.utils.source
import geowarin.bootwebpack.webpack.WebpackCompilerOptions
import geowarin.bootwebpack.webpack.Page
import org.amshove.kluent.shouldContain
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.io.File

class WebpackCompilerWatchTest {

    /**
     * This test is flaky because webpack sometimes doesn't pick changes if they occur too fast.
     * This is why I had introduced a sleep between two writes.
     *
     * It has nothing to do with the watcher nor the CachedInputFileSystem that webpack uses.
     * Reducing the aggregateTimeout of the watcher has no effect either because it
     * does picks up all the changes and invalidates the cache anyway.
     *
     * Painful sessions of debugging point to the NormalModule not rebuilding (NormalModule.build())
     * and the changes ending up not being processed by babel.
     */
    @Test(timeout = 120000)
    fun watchAsync() {
        val rootDir = tmpDir()
        val tmpPage = createFileInTmpDir(contentPath = "watch/page1.js", rootDir = rootDir)

        val options = WebpackCompilerOptions(
                watchDirectories = listOf(rootDir.canonicalPath),
                pages = listOf(Page(file = tmpPage, name = "page1"))
        )

        val watchObservable = createTestCompiler().watchAsync(options)

        for (i in (0..5)) {
            tmpPage.changeContents(newContentPath = "watch/page1.js")
            watchObservable.blockingFirst().source("page1.js") shouldContain "Page 1"

            tmpPage.changeContents(newContentPath = "watch/page2.js")
            watchObservable.blockingFirst().source("page1.js") shouldContain "Page 2"
        }
    }
}

fun File.changeContents(newContentPath: String) {
    // FIXME: one day...
    Thread.sleep(600)
    val contentsFile = ClassPathResource(newContentPath).file
    this.writeText(contentsFile.readText())
}

fun tmpDir(): File {
    val tempDir = createTempDir()
    tempDir.deleteOnExit()
    return tempDir
}

fun createFileInTmpDir(contentPath: String, rootDir: File = tmpDir()): File {
    val sourceFile = ClassPathResource(contentPath).file
    val createdFile = File(rootDir, sourceFile.name)
    createdFile.createNewFile()
    createdFile.writeText(sourceFile.readText())
    return createdFile
}

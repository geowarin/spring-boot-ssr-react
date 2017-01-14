package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.extensions.path.createFile
import geowarin.bootwebpack.extensions.path.div
import geowarin.bootwebpack.extensions.path.writeText
import geowarin.bootwebpack.utils.source
import org.amshove.kluent.shouldContain
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

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
                bootSsrDirectory = File("/Users/geowarin/dev/projects/boot-wp/react/boot-ssr"),
                watchDirectories = listOf(rootDir),
                pages = listOf(Page(path = tmpPage, name = "page1"))
        )

        val watchObservable = WebpackCompiler().watchAsync(options)

        for (i in (0..5)) {
            tmpPage.changeContents(newContentPath = "watch/page1.js")
            watchObservable.blockingFirst().source("page1.js") shouldContain "Page 1"

            tmpPage.changeContents(newContentPath = "watch/page2.js")
            watchObservable.blockingFirst().source("page1.js") shouldContain "Page 2"
        }
    }
}

fun Path.changeContents(newContentPath: String) {
    // FIXME: one day...
    Thread.sleep(600)
    val contentsFile = ClassPathResource(newContentPath).file
    this.writeText(contentsFile.readText())
}

fun tmpDir(): Path {
    val tempDir = Files.createTempDirectory("test")
    tempDir.toFile().deleteOnExit()
    return tempDir
}

fun createFileInTmpDir(contentPath: String, rootDir: Path = tmpDir()): Path {
    val sourceFile = ClassPathResource(contentPath).file
    val createdFile = rootDir / sourceFile.name
    createdFile.createFile()
    createdFile.writeText(sourceFile.readText())
    return createdFile
}

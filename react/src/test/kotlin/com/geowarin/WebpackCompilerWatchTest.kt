package com.geowarin

import com.geowarin.utils.createTestCompiler
import com.geowarin.utils.shouldContainAssets
import org.amshove.kluent.shouldContain
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.attribute.FileTime

class WebpackCompilerWatchTest {

    @Test(timeout = 10000)
    fun watchAsync() {

        val rootDir = createTempDir()
        val tmpPage = createInTempDir(contentPath = "watch/page1.js", rootDir = rootDir)

        val watchObservable = createTestCompiler(tmpPage).watchAsync(rootDir)

        val firstCompilation = watchObservable.blockingFirst()
        firstCompilation shouldContainAssets listOf("client.js", "renderer.js", "page1.js", "common.js")

        tmpPage.changeContents(newContentPath = "watch/page2.js")

        val secondCompilation = watchObservable.blockingFirst()

        secondCompilation shouldContainAssets listOf("client.js", "renderer.js", "page1.js", "common.js")
        secondCompilation.assets.find { it.name == "page1.js" }!!.source shouldContain "Page 2"
    }
}

fun File.changeContents(newContentPath: String) {
    val contentsFile = ClassPathResource(newContentPath).file
    this.writeText(contentsFile.readText())
}

fun createInTempDir(contentPath: String, rootDir:File = createTempDir()): File {
    rootDir.deleteOnExit()

    val sourceFile = ClassPathResource(contentPath).file
    val createdFile = File(rootDir, sourceFile.name)
    createdFile.createNewFile()
    createdFile.writeText(sourceFile.readText())
    // prevents webpack watcher bugs
//    Files.setLastModifiedTime(createdFile.toPath(), FileTime.fromMillis(0))

    return createdFile
}

package com.geowarin

import com.geowarin.utils.createTestCompiler
import com.geowarin.utils.shouldContainAssets
import org.amshove.kluent.shouldContain
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime

class WebpackCompilerWatchTest {

    @Test
    fun watchAsync() {

        val tmpPage = createInTempDir(contentPath = "watch/page1.js")
        val watchObservable = createTestCompiler(tmpPage).watchAsync()

        val firstCompilation = watchObservable.blockingFirst()
        firstCompilation shouldContainAssets listOf("client.js", "renderer.js", "page1.js", "common.js")

        tmpPage.changeContents(newContentPath = "watch/page2.js")

        val secondCompilation = watchObservable.blockingFirst()

        secondCompilation shouldContainAssets listOf("client.js", "renderer.js", "page1.js", "common.js")
        secondCompilation.assets.find { it.name == "page1.js" }!!.source shouldContain "Page 2"
    }

    @Test
    fun testSegfault() {

        val tmpPage = createInTempDir(contentPath = "watch/page1.js")
        val watchObservable = createTestCompiler(tmpPage).watchAsync()

        watchObservable.blockingSubscribe()

    }
}

fun File.changeContents(newContentPath: String) {
    val contentsFile = ClassPathResource(newContentPath).file
    this.writeText(contentsFile.readText())
}

fun createInTempDir(contentPath: String): File {
    val rootDir = createTempDir()
    rootDir.deleteOnExit()

    val sourceFile = ClassPathResource(contentPath).file
    val createdFile = File(rootDir, sourceFile.name)
    createdFile.createNewFile()
    createdFile.writeText(sourceFile.readText())
    // prevents webpack watcher bugs
    Files.setLastModifiedTime(createdFile.toPath(), FileTime.fromMillis(0))

    return createdFile
}

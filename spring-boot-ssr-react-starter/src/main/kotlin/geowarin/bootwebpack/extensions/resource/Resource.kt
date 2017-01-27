package geowarin.bootwebpack.extensions.resource

import geowarin.bootwebpack.extensions.path.toPath
import org.springframework.core.io.Resource
import java.nio.charset.Charset
import java.nio.file.Path

fun Resource.readText(charset: Charset = Charsets.UTF_8): String {
    return this.inputStream.bufferedReader(charset).readText()
}

fun Resource.readBytes(): ByteArray {
    return inputStream.readBytes()
}

fun Resource.relativizePath(root: Resource): String {
    return root.cleanJarUrlPath().relativize(this.cleanJarUrlPath()).toString()
}

fun Resource.cleanJarUrlPath(): Path {
    return this.uri.toString().split("!").last().toPath()
}
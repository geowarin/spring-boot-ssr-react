package geowarin.bootwebpack.extensions.resource

import org.springframework.core.io.Resource
import java.nio.charset.Charset

fun Resource.readText(charset: Charset = Charsets.UTF_8): String {
    return this.inputStream.bufferedReader(charset).readText()
}

fun Resource.readBytes(): ByteArray {
    return inputStream.readBytes()
}
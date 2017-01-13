package geowarin.bootwebpack.extensions

import java.io.File

fun File.pathWithoutExt(): String {
    return this.path.replaceAfterLast(".", "").dropLast(1)
}

fun String.withoutExt(): String {
    return this.replaceAfterLast(".", "").dropLast(1)
}
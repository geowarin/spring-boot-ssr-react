package geowarin.bootwebpack.extensions.path

import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Opens this path for reading.
 *
 * See [Files.bufferedReader] for complete documentation.
 */
fun Path.bufferedReader(charset: Charset = StandardCharsets.UTF_8): BufferedReader = Files.newBufferedReader(this, charset)

/**
 * Opens this path for writing.
 *
 * See [Files.bufferedWriter] for complete documentation.
 */
fun Path.bufferedWriter(charset: Charset = StandardCharsets.UTF_8, vararg options: OpenOption): BufferedWriter =
        Files.newBufferedWriter(this, charset, *options)
/**
 * Returns a new [PrintWriter] for writing the content of this file.
 */
fun Path.printWriter(charset: Charset = Charsets.UTF_8): PrintWriter = PrintWriter(bufferedWriter(charset))

/**
 * Gets the entire content of this file as a byte array.
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB byte array size.
 *
 * @return the entire content of this file as a byte array.
 */
fun Path.readBytes(): ByteArray = Files.readAllBytes(this)

/**
 * Sets the content of this file as an [array] of bytes.
 * If this file already exists, it becomes overwritten.
 *
 * @param array byte array to write into this file.
 */
fun Path.writeBytes(array: ByteArray): Path = Files.write(this, array)

/**
 * Appends an [array] of bytes to the content of this file.
 *
 * @param array byte array to append to this file.
 */
fun Path.appendBytes(array: ByteArray): Path = Files.write(this, array, StandardOpenOption.APPEND)

/**
 * Gets the entire content of this file as a String using UTF-8 or specified [charset].
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
 *
 * @param charset character set to use.
 * @return the entire content of this file as a String.
 */
fun Path.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Sets the content of this file as [text] encoded using UTF-8 or specified [charset].
 * If this file exists, it becomes overwritten.
 *
 * @param text text to write into file.
 * @param charset character set to use.
 */
fun Path.writeText(text: String, charset: Charset = Charsets.UTF_8): Path = writeBytes(text.toByteArray(charset))

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use.
 */
fun Path.appendText(text: String, charset: Charset = Charsets.UTF_8): Path = appendBytes(text.toByteArray(charset))

/**
 * Reads this file line by line using the specified [charset] and calls [action] for each line.
 * Default charset is UTF-8.
 *
 * You may use this function on huge files.
 *
 * @param charset character set to use.
 * @param action function to process file lines.
 */
fun Path.forEachLine(charset: Charset = Charsets.UTF_8, action: (line: String) -> Unit): Unit {
    // Note: close is called at forEachLine
    bufferedReader(charset).forEachLine(action)
}

/**
 * Constructs a new FileInputStream of this file and returns it as a result.
 */
inline fun Path.inputStream(): InputStream = Files.newInputStream(this)

/**
 * Constructs a new FileOutputStream of this file and returns it as a result.
 */
inline fun Path.outputStream(vararg options: OpenOption): OutputStream? = Files.newOutputStream(this, *options)

/**
 * Reads the file content as a list of lines.
 *
 * Do not use this function for huge files.
 *
 * @param charset character set to use. By default uses UTF-8 charset.
 * @return list of file lines.
 */
fun Path.readLines(charset: Charset = Charsets.UTF_8): List<String> = Files.readAllLines(this, charset)

/**
 * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
 * the processing is complete.

 * @param charset character set to use. By default uses UTF-8 charset.
 * @return the value returned by [block].
 */
inline fun <T> Path.useLines(charset: Charset = Charsets.UTF_8, block: (Sequence<String>) -> T): T =
        bufferedReader(charset).use { block(it.lineSequence()) }

package geowarin.bootwebpack.v8

import geowarin.bootwebpack.webpack.Asset
import geowarin.bootwebpack.webpack.AssetStore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class V8ScriptTest {
    @Rule @JvmField
    val thrown: ExpectedException = ExpectedException.none()

    @Test
    fun testConsole() {
        val source = """ console.log("hello", "world") """
        setup(source)
    }

    @Test
    fun testAssert() {
        thrown.expectMessage("hello world")
        val source = """ console.assert(false, "%s world", "hello") """
        setup(source)
    }

    @Test
    fun testAssertWithoutMessage() {
        thrown.expectMessage("Assertion Error")
        val source = """ console.assert(false) """
        setup(source)
    }

    @Test
    fun testDir() {
        val source = """
var obj = {key: "value"}
console.dir(obj)
"""
        setup(source)
    }

    @Test
    fun testTime() {
        val source = """ console.timeEnd("label") """
        setup(source)
    }

    private fun setup(source: String) {
        val assetStore = AssetStore()
        assetStore.store(listOf(Asset(
                name = "testConsole.js",
                source = source
        )))

        val v8Script = V8Script(assetStore)
        v8Script.execute("testConsole.js")
    }
}
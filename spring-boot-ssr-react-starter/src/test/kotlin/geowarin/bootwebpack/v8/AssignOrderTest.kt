package geowarin.bootwebpack.v8

import com.eclipsesource.v8.V8
import geowarin.bootwebpack.extensions.resource.readText
import org.amshove.kluent.shouldEqual
import org.junit.Test
import org.springframework.core.io.ClassPathResource

// https://bugs.chromium.org/p/v8/issues/detail?id=3056
val bugTest = """
var test3 = {};
'abcdefghijklmnopqrst'.split('').forEach( (letter) => test3[letter] = letter );
var buggy = Object.keys(Object.assign({}, test3)).join('') !== 'abcdefghijklmnopqrst'
"""

val polyfill = ClassPathResource("object.assign.polyfill.js").readText()

/**
 * Node v < 6 embeds a V8 version which returns properties in the wrong order
 * (not spec compliant) after Object.assign().
 *
 * This is problematic when reconciling the client DOM with the server side
 * dom. See: https://github.com/facebook/react/issues/6451
 */
class AssignOrderTest {

    @Test
    fun polyfill_should_fix_the_bug() {
        val v8Runtime = V8.createV8Runtime()

        v8Runtime.executeScript(polyfill)

        v8Runtime.executeScript(bugTest)
        v8Runtime.getBoolean("buggy") shouldEqual false
    }

    @Test
    fun V8_has_an_ordering_problem() {
        val v8Runtime = V8.createV8Runtime()

        v8Runtime.executeScript(bugTest)
        // if we update to node 6+, we should be able to remove the polyfill
        v8Runtime.getBoolean("buggy") shouldEqual true
    }
}
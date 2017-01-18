package geowarin.bootwebpack.v8

import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldHaveKey
import org.junit.Assert.fail
import org.junit.Test

class V8ConvertibleTest {

    data class SimpleObject(val name: String) :
            V8Convertible<SimpleObject>({ "name" mappedBy it.name })

    data class Container(val simpleObject: SimpleObject) :
            V8Convertible<Container>({ "obj" mappedBy it.simpleObject })

    data class CollectionContainer(val objs: Iterable<SimpleObject>) :
            V8Convertible<CollectionContainer>({ "objs" mappedBy it.objs })

    @Test
    fun shouldConvertSimpleObjectToMap() {
        val mapResult = SimpleObject(name = "haskell").toMap()

        mapResult shouldBeInstanceOf Map::class.java
        mapResult shouldContain ("name" to "haskell")
    }

    @Test
    fun shouldConvertIterableToMap() {
        val mapResult = CollectionContainer(objs = listOf(SimpleObject("dagobert"))).toMap()

        mapResult shouldBeInstanceOf Map::class.java
        mapResult shouldHaveKey "objs"
        val objs = mapResult["objs"]
        if (objs is Iterable<*>) {
            objs.first() shouldBeInstanceOf Map::class.java
            (objs.first() as Map<String, *>)["name"] shouldEqual "dagobert"
        } else {
            fail("should be an iterable")
        }
    }

    @Test
    fun shouldConvertNestedObjectsToMap() {
        val mapResult = Container(SimpleObject(name = "Alonso")).toMap()

        mapResult shouldBeInstanceOf Map::class.java
        mapResult shouldHaveKey "obj"
        val nested = mapResult["obj"]
        if (nested is Map<*, *>) {
            nested["name"] shouldEqual "Alonso"
        } else {
            fail("Nested object is not a map")
        }
    }
}
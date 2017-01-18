package geowarin.bootwebpack.v8

infix fun <A, B : V8Convertible<*>> A.mappedBy(that: B): Pair<A, Any> = Pair(this, that.toMap())
infix fun <A, B> A.mappedBy(that: B): Pair<A, B> = Pair(this, that)
infix fun <A, B : Iterable<V8Convertible<*>>> A.mappedBy(that: B): Pair<A, Any> = Pair(this, that.map { it.toMap() })

abstract class V8Convertible<T>(vararg val props: (T) -> Pair<String, Any?>) {

    // FIXME: reify ? https://kotlinlang.org/docs/reference/inline-functions.html#reified-type-parameters
    @Suppress("UNCHECKED_CAST")
    fun getThis(): T {
        return this as T
    }

    fun toMap(): Map<String, *> {
        return props.map { it.invoke(getThis()) }.toMap()
    }
}
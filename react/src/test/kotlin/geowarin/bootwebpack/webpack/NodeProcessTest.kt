package geowarin.bootwebpack.webpack

import geowarin.bootwebpack.utils.asTmpFile
import geowarin.bootwebpack.v8.V8Convertible
import geowarin.bootwebpack.v8.mappedBy
import org.junit.Test
import java.io.File

class NodeProcessTest {

    data class Person(val name: String) :
            V8Convertible<Person>({ "name" mappedBy it.name })

    @Test
    fun shouldAddV8ConvertibleToRuntime() {
        val person = Person(name = "Edward")

        val script = """ console.assert(person.name == 'Edward') """.asTmpFile()
        val nodeProcess = NodeProcess(script)

        nodeProcess.addObj("person", person)
        nodeProcess.startSync()
    }
}
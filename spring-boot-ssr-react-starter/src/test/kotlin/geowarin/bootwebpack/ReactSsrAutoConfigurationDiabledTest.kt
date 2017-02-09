package geowarin.bootwebpack

import geowarin.bootwebpack.v8.V8ScriptTemplateViewResolver
import org.amshove.kluent.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@EnableAutoConfiguration
@SpringBootTest(
        classes = arrayOf(ReactSsrAutoConfiguration::class),
        properties = arrayOf("react.enabled=false")
)
class ReactSsrAutoConfigurationDiabledTest {

    @Autowired
    lateinit var context: ApplicationContext

    @Test
    fun `should be disabled if the property says so`() {
        val viewResolver = context.getBeansOfType(V8ScriptTemplateViewResolver::class.java)
        viewResolver.isEmpty() shouldBe true
    }
}
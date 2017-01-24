package geowarin.bootwebpack

import geowarin.bootwebpack.v8.V8ScriptTemplateViewResolver
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration

@RunWith(SpringJUnit4ClassRunner::class)
@EnableAutoConfiguration
@WebAppConfiguration
class ReactSsrAutoConfigurationTest {

    @Autowired
    lateinit var context: ApplicationContext

    @Test
    fun toto() {

        val viewResolver = context.getBean(V8ScriptTemplateViewResolver::class.java)
        viewResolver shouldNotBe null
    }
}
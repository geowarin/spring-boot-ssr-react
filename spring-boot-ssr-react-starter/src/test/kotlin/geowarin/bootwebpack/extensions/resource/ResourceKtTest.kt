package geowarin.bootwebpack.extensions.resource

import org.amshove.kluent.shouldEqual
import org.junit.Test
import org.springframework.core.io.ClassPathResource

class ResourceKtTest {

    @Test
    fun should_handle_classpath_resources() {

        val resource = ClassPathResource("/templates/index.html")
        val root = ClassPathResource("/templates")
        resource.relativizePath(root) shouldEqual "index.html"
    }
}
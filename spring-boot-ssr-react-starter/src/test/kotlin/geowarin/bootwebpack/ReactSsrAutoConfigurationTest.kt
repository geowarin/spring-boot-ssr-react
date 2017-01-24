package geowarin.bootwebpack

import geowarin.bootwebpack.config.BootSsrConfigurationFactory
import geowarin.bootwebpack.utils.FileSystemRule
import geowarin.bootwebpack.v8.V8ScriptTemplateViewResolver
import geowarin.bootwebpack.webpack.Page
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.junit.Rule
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

    @Rule @JvmField val fs = FileSystemRule()

    @Test
    fun with_valid_configuration() {
        val projectDirectory = fs.createPath("myProjectDir")
        val jsDir = fs.createPath("myProjectDir/src/main/js")
        val bootSsrNodeModule = fs.createPath("myProjectDir/src/main/js/node_modules/spring-boot-ssr-react-node")

        val homePage = fs.createPath("myProjectDir/src/main/js/pages/home.js")
        val subPage = fs.createPath("myProjectDir/src/main/js/pages/sub/subPage.js")


        val viewResolver = context.getBean(V8ScriptTemplateViewResolver::class.java)
        viewResolver shouldNotBe null

        val configFactory = context.getBean(BootSsrConfigurationFactory::class.java)
        configFactory.initializeProjectDir(projectDirectory)

        val (webpackCompilerOptions, additionalBuildInfo) = configFactory.create()
        additionalBuildInfo.pagesDir shouldEqual fs.getPath("myProjectDir/src/main/js/pages")

        webpackCompilerOptions.pages shouldEqual
                listOf(
                        Page(name = "home", path = homePage),
                        Page(name = "sub/subPage", path = subPage)
                )
    }
}
package geowarin.bootwebpack.config

import geowarin.bootwebpack.utils.FileSystemRule
import geowarin.bootwebpack.webpack.Page
import org.amshove.kluent.shouldEqual
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class BootSsrConfigurationFactoryTest {

    @Rule @JvmField val rule: FileSystemRule = FileSystemRule()

    @Rule @JvmField
    val thrown: ExpectedException = ExpectedException.none()

    @Test
    fun getPages() {
        val page1 = rule.createPath("pagesDir/page1.js")
        val subPage = rule.createPath("pagesDir/sub/subPage.js")

        val pages = configFactory().getPages(pagesDir = rule.getPath("pagesDir"))

        pages.map(Page::name) shouldEqual listOf("page1", "sub/subPage")
        pages.map(Page::path) shouldEqual listOf(page1, subPage)
    }

    @Test
    fun checkPagesDirectory() {
        // the path does not exist
        val pagesDirPath = rule.getPath("myProject/js/pages")
        val checkedPageDir = configFactory().checkPagesDirectory(pagesDirPath)

        checkedPageDir shouldEqual pagesDirPath
        // TODO: assert warning
    }

    private fun configFactory(): BootSsrConfigurationFactory {
        val configFactory = BootSsrConfigurationFactory(ReactSsrProperties())
        return configFactory
    }

    @Test
    fun checkNodeModulePathShouldResolveRelativePathsToJsSource() {
        val jsSourceDir = rule.createPath("jsSourceDir")
        val relativeNodeModulePath = rule.getPath("src/main/js")
        val expectedPath = rule.createPath("jsSourceDir/src/main/js")

        val checkedNodeModulePath = configFactory().checkNodeModulePath(jsSourceDir, relativeNodeModulePath.toString())
        checkedNodeModulePath shouldEqual expectedPath
    }

    @Test
    fun checkNodeModulePathShouldResolveAbsolutePath() {
        val jsSourceDir = rule.createPath("whatever")
        val absoluteNodeModulePath = rule.createPath("/myProject/src/main/js")

        val checkedNodeModulePath = configFactory().checkNodeModulePath(jsSourceDir, absoluteNodeModulePath.toString())
        checkedNodeModulePath shouldEqual absoluteNodeModulePath
    }

    @Test
    fun checkNodeModulePathShouldThrowIfMissing() {
        thrown.expect(IllegalStateException::class.java)

        val jsSourceDir = rule.createPath("whatever")
        val absoluteNodeModulePath = "/myProject/src/main/js"

        configFactory().checkNodeModulePath(jsSourceDir, absoluteNodeModulePath)
    }
}
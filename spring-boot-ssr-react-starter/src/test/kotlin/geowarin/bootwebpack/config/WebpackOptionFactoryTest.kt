package geowarin.bootwebpack.config

import geowarin.bootwebpack.utils.FileSystemRule
import geowarin.bootwebpack.webpack.Page
import org.amshove.kluent.shouldEqual
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class WebpackOptionFactoryTest {

    @Rule @JvmField val rule: FileSystemRule = FileSystemRule()

    @Rule @JvmField
    val thrown: ExpectedException = ExpectedException.none()

    @Test
    fun getPages() {
        val page1 = rule.createPath("pagesDir/page1.js")
        val subPage = rule.createPath("pagesDir/sub/subPage.js")

        val pages = WebpackOptionFactory().getPages(pagesDir = rule.getPath("pagesDir"))

        pages.map(Page::name) shouldEqual listOf("page1", "sub/subPage")
        pages.map(Page::path) shouldEqual listOf(page1, subPage)
    }

    @Test
    fun checkPagesDirectory() {
        // the path does not exist
        val pagesDirPath = rule.getPath("myProject/js/pages")
        val checkedPageDir = WebpackOptionFactory().checkPagesDirectory(pagesDirPath)

        checkedPageDir shouldEqual pagesDirPath
        // TODO: assert warning
    }

    @Test
    fun checkNodeModulePathShouldResolveRelativePathsToJsSource() {
        val jsSourceDir = rule.createPath("jsSourceDir")
        val relativeNodeModulePath = rule.getPath("src/main/js")
        val expectedPath = rule.createPath("jsSourceDir/src/main/js")

        val checkedNodeModulePath = WebpackOptionFactory().checkNodeModulePath(jsSourceDir, relativeNodeModulePath)
        checkedNodeModulePath shouldEqual expectedPath
    }

    @Test
    fun checkNodeModulePathShouldResolveAbsolutePath() {
        val jsSourceDir = rule.createPath("whatever")
        val absoluteNodeModulePath = rule.createPath("/myProject/src/main/js")

        val checkedNodeModulePath = WebpackOptionFactory().checkNodeModulePath(jsSourceDir, absoluteNodeModulePath)
        checkedNodeModulePath shouldEqual absoluteNodeModulePath
    }

    @Test
    fun checkNodeModulePathShouldThrowIfMissing() {
        thrown.expect(IllegalStateException::class.java)

        val jsSourceDir = rule.createPath("whatever")
        val absoluteNodeModulePath = rule.getPath("/myProject/src/main/js")

        WebpackOptionFactory().checkNodeModulePath(jsSourceDir, absoluteNodeModulePath)
    }

    @Test
    fun checkJsSourceDir() {

    }

}
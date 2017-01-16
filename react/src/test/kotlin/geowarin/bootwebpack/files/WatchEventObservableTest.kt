package geowarin.bootwebpack.files

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.WatchServiceConfiguration
import geowarin.bootwebpack.utils.FileSystemRule
import io.reactivex.schedulers.Schedulers
import org.amshove.kluent.shouldEqual
import org.junit.Rule
import org.junit.Test
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class WatchEventObservableTest {

    @Rule @JvmField val rule = FileSystemRule(fastPollingConfig())

    private fun fastPollingConfig(): Configuration {
        return Configuration.forCurrentPlatform().toBuilder()
                .setWatchServiceConfiguration(WatchServiceConfiguration.polling(100, TimeUnit.MILLISECONDS))
                .build()
    }

    @Test
    fun testWatchSimpleObservableShouldReceiveSingularEvents() {

        val pagesDir = rule.createPath("pageDir")

        val observable = WatchEventObservable.addAndDeleteWatcher(pagesDir)

        rule.createPath("pageDir/page1.js")
        rule.createPath("pageDir/page2.js")

        var count = 0
        val subscription = observable
                .subscribeOn(Schedulers.io())
                .subscribe { count++ }

        Thread.sleep(500)
        subscription.dispose()

        count shouldEqual 1
    }
}
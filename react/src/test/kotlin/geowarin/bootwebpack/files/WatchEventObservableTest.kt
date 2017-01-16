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
    fun testWatchObservableShouldReceiveCreateEvents() {

        val pagesDir = rule.createPath("pageDir")

        val watcher = pagesDir.watchService()
        val observable = WatchEventObservable.create(watcher)

        rule.createPath("pageDir/page1.js")
        rule.createPath("pageDir/page2.js")

        val watchEvents = observable.take(2).blockingIterable().toList().sortedBy { it.context() as Path }

        watchEvents[0].context() shouldEqual rule.getPath("page1.js")
        watchEvents[1].context() shouldEqual rule.getPath("page2.js")
    }

    @Test
    fun testWatchSimpleObservableShouldReceiveSingularEvents() {

        val pagesDir = rule.createPath("pageDir")
        val watcher = pagesDir.watchService()

        rule.createPath("pageDir/page1.js")
        rule.createPath("pageDir/page2.js")

        val observable = WatchEventObservable.createSimple(watcher)

        var count = 0
        val subscription = observable
                .subscribeOn(Schedulers.io())
                .subscribe { count++ }

        Thread.sleep(500)
        subscription.dispose()

        count shouldEqual 1
    }
}
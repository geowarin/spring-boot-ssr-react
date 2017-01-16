package geowarin.bootwebpack.files

import com.sun.nio.file.SensitivityWatchEventModifier
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.WatchEvent
import java.nio.file.WatchService

internal enum class Nothing {
    VOID
}

class WatchEventObservable {

    companion object Factory {

        fun create(watchService: WatchService): Flowable<WatchEvent<*>> {
            return Flowable.create({ emitter: FlowableEmitter<WatchEvent<*>> ->
                while (!emitter.isCancelled) {
                    val key = watchService.poll()
                    if (key != null) {
                        key.pollEvents().forEach { event ->
                            emitter.onNext(event)
                        }
                        key.reset()
                    }
                }
//                emitter.setCancellable { }
            }, BackpressureStrategy.BUFFER)
        }

        fun createSimple(watchService: WatchService): Flowable<Any> {
            return Flowable.create({ emitter: FlowableEmitter<Any> ->
                while (!emitter.isCancelled) {
                    val key = watchService.poll()
                    if (key != null) {
                        if (key.pollEvents().size > 0) {
                            emitter.onNext(Nothing.VOID)
                        }
                        key.reset()
                    }
                }
            }, BackpressureStrategy.BUFFER)
        }

    }
}

fun Path.watchService(): WatchService {
    val watcher = this.fileSystem.newWatchService()
    val twoSecondsPolling = SensitivityWatchEventModifier.HIGH
    this.register(watcher, kotlin.arrayOf(ENTRY_CREATE, ENTRY_DELETE), twoSecondsPolling)
    return watcher
}
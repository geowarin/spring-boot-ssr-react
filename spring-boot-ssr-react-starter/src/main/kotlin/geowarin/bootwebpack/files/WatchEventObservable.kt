package geowarin.bootwebpack.files

import com.sun.nio.file.SensitivityWatchEventModifier
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.WatchService

internal enum class Nothing {
    VOID
}

class WatchEventObservable {

    companion object Factory {

        /**
         * Returns an observable which will react to each addition/deletion in a directory.
         * Aggregated changes (during a 2 second poll-period) will be merged.
         * File paths are not sent.
         */
        fun addAndDeleteWatcher(path: Path): Flowable<Any> {

            val watchService = path.watchService()
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
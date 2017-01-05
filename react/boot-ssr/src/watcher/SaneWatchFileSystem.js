"use strict";

const createDebug = require('debug');
const Watcher = require('./SaneWatcher');
const debug = createDebug('watchman:filesystem');

class SaneWatchFileSystem {

  /**
   *
   * @param inputFileSystem
   * @param options: {Options}
   */
  constructor(inputFileSystem, options) {
    this.inputFileSystem = inputFileSystem;
    this.options = options;
  }

  /**
   *
   * @param files: Array<string>
   * @param dirs: Array<string>
   * @param missing: Array<string>
   * @param startTime: number: Object
   * @param options:{Options}
   * @param callback: Callback
   * @param callbackUndelayed: (file: string, mtime: number) => void
   * @return {{close: (function(): *), pause: (function(): *)}}
   */
  watch(files, dirs, missing, startTime, options, callback, callbackUndelayed) {
    const oldWatcher = this.watcher;

    debug('creating new connector');
    this.watcher = new Watcher(Object.assign({}, options, this.options));

    if (callbackUndelayed) {
      this.watcher.once('change', (filePath, mtime) => {
        debug('change event received for %s with mtime', filePath, mtime);
        callbackUndelayed(filePath, mtime);
      });
    }

    this.watcher.once('aggregated', changes => {
      debug('aggregated event received with changes: ', changes);
      if (this.inputFileSystem && this.inputFileSystem.purge) {
        this.inputFileSystem.purge(changes);
      }

      const times = this.watcher.getTimes();

      callback(
        null,
        changes.filter(file => files.indexOf(file) >= 0).sort(),
        changes.filter(file => dirs.indexOf(file) >= 0).sort(),
        changes.filter(file => missing.indexOf(file) >= 0).sort(),
        times,
        times
      );
    });

    this.watcher.watch(files.concat(missing), dirs, startTime);

    if (oldWatcher) {
      debug('closing old connector');
      oldWatcher.close();
    }

    return {
      close: () => this.watcher.close(),
      pause: () => this.watcher.pause(),
    };
  }
}

module.exports = SaneWatchFileSystem;

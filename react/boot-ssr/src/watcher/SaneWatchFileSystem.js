"use strict";

var createDebug = require('debug');
var Watcher = require('./SaneWatcher');

const debug = createDebug('watchman:filesystem');

/*
type Options = { projectPath: string };
type Callback = (
  err: ?Error,
  files: Array<string>,
  dirs: Array<string>,
  missing: Array<string>,
  filetimes: { [key: string]: number },
  dirtimes: { [key: string]: number },
) => void;
*/

class WatchmanWatchFileSystem {

/*
  inputFileSystem;
  /!**
   * @type {Options}
   *!/
  options;
  watcher;
  /!**
   * @type {string}
   *!/
  lastClock;
*/

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

    this.watcher.once('aggregated', (changes, clock) => {
      this.lastClock = clock;
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

    this.watcher.watch(files.concat(missing), dirs, this.lastClock || startTime);

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

module.exports = WatchmanWatchFileSystem;

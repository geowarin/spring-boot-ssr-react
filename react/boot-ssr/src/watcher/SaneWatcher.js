"use strict";

const async = require('async');
const EventEmitter = require('events');
const fs = require('fs');
const path = require('path');
const sane = require('sane');
const fsAccurency = require('./utils/fsAccurency');

/**
 * A sane watcher that watches multiple direcotries
 */
class MultiWatcher extends EventEmitter {
  constructor(directories, options) {
    super();
    this.watchers = directories.map(d => {
      const watcher = sane(d, options);
      watcher.on('ready', () => this.emit('ready'));
      watcher.on('change', (filepath, root, stat) => this.emit('change', filepath, root, stat));
      watcher.on('add', (filepath, root, stat) => this.emit('add', filepath, root, stat));
      watcher.on('delete', (filepath, root) => this.emit('delete', filepath, root));
      return watcher;
    });
  }

  close() {
    this.watchers.forEach(w => w.close());
  }
}

class SaneWatcher extends EventEmitter {

  //: Options = { aggregateTimeout: 200, projectPath: '' }
  constructor(options) {
    super();
    if (!Array.isArray(options.watchDirectories)) {
      throw new Error('"watchDirectories" option is missing for the WatcherPlugin');
    }

    this.options = options;
    this.aggregatedChanges = [];
    this.fileTimes = {};
    this.paused = true;
    this.timeoutRef = 0;
    this.isWatching = false;
  }

  /**
   * @param files:Array<string>
   * @param dirs:Array<string>
   * @param since:string|number
   * @param done?: () => void
   *
   * `since` has to be either a string with a watchman clock value, or a number
   * which is then treated as a timestamp in milliseconds
   */
  watch(files, dirs, since, done) {
    this.paused = false;

    const allFiles = files.concat(dirs);

    if (!this.isWatching) {
      this.isWatching = true;
      this._startWatch(allFiles, since, () => {});
    }
  }

  /**
   * { [key: string]: number }
   * @return
   */
  getTimes() {
    return this.fileTimes;
  }

  close() {
    this.paused = true;
    if (this.timeoutRef) {
      clearTimeout(this.timeoutRef);
    }

    if (this.watcher) {
      this.watcher.close();
      this.watcher = null;
    }
  }

  pause() {
    this.paused = true;
    if (this.timeoutRef) {
      clearTimeout(this.timeoutRef);
    }
  }

  /**
   * @param files: Array<string>
   * @param since: string|number
   * @param done: () => void
   * @private
   */
  _startWatch(files, since, done) {
    const options = {glob: ['**/*.js', '**/*.css']};
    this.watcher = new MultiWatcher(this.options.watchDirectories, options);

    this.watcher.on('ready', function () {
      done();
    });
    this.watcher.on('change', (filepath, root, stat) => {
      const filePath = path.join(root, filepath);
      this._onFile(filePath, stat);
    });
    this.watcher.on('add', (filepath, root, stat) => {
      const filePath = path.join(root, filepath);
      this._onFile(filePath, stat);
    });
    this.watcher.on('delete', (filepath, root) => {
      const filePath = path.join(root, filepath);
      this._onFile(filePath, null);
    });
  }

  _onFile(filePath, stat) {
    const mtime = stat ? +stat.mtime : null;
    this._setFileTime(filePath, mtime);

    // FIXME : webpack doesn't call pause but this code looks wrong
    if (this.paused || !mtime) {
      console.log("ignored !", filePath);
      return;
    }

    this._handleEvents(filePath, mtime);
  }

  /**
   * @param file: string
   * @param mtime: ?number
   * @private
   */
  _setFileTime(file, mtime) {
    this.fileTimes[file] = mtime;
  }

  /**
   * @param filePath: string
   * @param mtime: ?number
   * @private
   */
  _handleEvents(filePath, mtime) {
    this.emit('change', filePath, mtime);

    this._handleAggregated(filePath);
  }

  /**
   * @param file: string
   * @private
   */
  _handleAggregated(file) {
    if (this.timeoutRef) {
      clearTimeout(this.timeoutRef);
    }

    if (this.aggregatedChanges.indexOf(file) < 0) {
      this.aggregatedChanges.push(file);
    }

    this.timeoutRef = setTimeout(() => this._onTimeout(), this.options.aggregateTimeout);
  }

  _onTimeout() {
    this.timeoutRef = 0;
    const changes = this.aggregatedChanges;
    this.aggregatedChanges = [];

    this.emit('aggregated', changes);
  };

  /**
   * @param files: Array<string>
   * @param done: () => void
   * @private
   */
  _doInitialScan(files, done) {
    async.eachLimit(files, 500, (file, callback) => {
      fs.stat(file, (err, stat) => {
        if (err) {
          callback(err);
          return;
        }

        const mtime = +stat.mtime;
        fsAccurency.revalidate(mtime);

        this._setFileTime(file, mtime);
        callback();
      });
    }, () => {
      done();
    });
  }
}

module.exports = SaneWatcher;

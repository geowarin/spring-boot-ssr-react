"use strict";

const async = require('async');
const EventEmitter = require('events');
const fs = require('fs');
const path = require('path');
const sane = require('sane');

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

  watch(files, dirs, since) {
    this.paused = false;

    if (!this.isWatching) {
      this.isWatching = true;
      const allFiles = files.concat(dirs);
      this._startWatch(allFiles, since, () => {});
    } else {
      this.restoreTimeout();
    }
  }

  getTimes() {
    return this.fileTimes;
  }

  close() {
    this.pause();
    if (this.watcher) {
      this.watcher.close();
      this.watcher = null;
    }
  }

  pause() {
    this.paused = true;
    this.clearTimeout();
  }

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

    this.addAggregatedChange(filePath);

    if (this.paused || !mtime) {
      return;
    }

    this._handleEvents(filePath, mtime);
  }

  addAggregatedChange(filePath) {
    if (this.aggregatedChanges.indexOf(filePath) < 0) {
      this.aggregatedChanges.push(filePath);
    }
  }

  _setFileTime(file, mtime) {
    this.fileTimes[file] = mtime;
  }

  _handleEvents(filePath, mtime) {
    this.emit('change', filePath, mtime);
    this.clearTimeout();
    this.restoreTimeout();
  }

  clearTimeout() {
    if (this.timeoutRef) {
      clearTimeout(this.timeoutRef);
    }
  }

  restoreTimeout() {
    if (this.aggregatedChanges.length === 0) {
      return;
    }
    this.timeoutRef = setTimeout(() => this._onTimeout(), this.options.aggregateTimeout);
  }

  _onTimeout() {
    this.timeoutRef = 0;
    const changes = this.aggregatedChanges;
    this.aggregatedChanges = [];
    this.emit('aggregated', changes);
  };

  // This doesn't serve any purpose since webpack seems to not be
  // interested in getTimes()
  // We might want to uncomment this if this proves false
 /* _doInitialScan(files, done) {
    async.eachLimit(files, 500, (file, callback) => {
      fs.stat(file, (err, stat) => {
        if (err) {
          callback(err);
          return;
        }

        const mtime = +stat.mtime;

        this._setFileTime(file, mtime);
        callback();
      });
    }, () => {
      done();
    });
  }
  */
}

module.exports = SaneWatcher;

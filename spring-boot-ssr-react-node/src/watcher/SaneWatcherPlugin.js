"use strict";

const WatchFileSystem = require('./SaneWatchFileSystem');

/**
 * This exists because the current watcher for webpack (watchpack) segfaults
 * when launched in the JVM...
 * The SaneWatcher is much more straight to the point than watchpack implementation
 * because ask the user to pass a list of watchDirectories and we will watch only that
 */
class SaneWatcherPlugin {

  constructor(options) {
    this.options = options || {};
  }

  apply(compiler) {
    compiler.plugin('environment', () => {
      compiler.watchFileSystem = new WatchFileSystem(compiler.inputFileSystem, this.options);
    });
  }
}

module.exports = SaneWatcherPlugin;

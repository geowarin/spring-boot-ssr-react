"use strict";

const WatchFileSystem = require('./SaneWatchFileSystem');

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

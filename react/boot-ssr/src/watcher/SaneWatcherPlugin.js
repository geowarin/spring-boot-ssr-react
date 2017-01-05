"use strict";

const createDebug = require('debug');
const WatchFileSystem = require('./SaneWatchFileSystem');

const debug = createDebug('watchman:plugin');

class SaneWatcherPlugin {

  /**
   * @param options:{Options}
   */
  constructor(options) {
    this.options = options || {};
  }

  apply(compiler) {
    compiler.plugin('environment', () => {
      debug('creating new filesystem');
      compiler.watchFileSystem = new WatchFileSystem(compiler.inputFileSystem, this.options);
    });
  }
}

module.exports = SaneWatcherPlugin;

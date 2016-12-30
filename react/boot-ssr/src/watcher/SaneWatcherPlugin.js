"use strict";

var createDebug = require('debug')
var WatchFileSystem = require('./SaneWatchFileSystem')

const debug = createDebug('watchman:plugin');
/*
type Options = { projectPath: string };
*/

class SaneWatcherPlugin {

  /**
   * @param options:{Options}
   */
  constructor(options) {
    this.options = options || {};
    if (!this.options.projectPath) throw new Error('projectPath is missing for WatchmanPlugin');
  }

  apply(compiler) {
    compiler.plugin('environment', () => {
      debug('creating new filesystem');
      compiler.watchFileSystem = new WatchFileSystem(compiler.inputFileSystem, this.options);
    });
  }
}

module.exports = SaneWatcherPlugin;

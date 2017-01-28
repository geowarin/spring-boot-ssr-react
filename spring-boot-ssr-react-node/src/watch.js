'use strict';

const path = require('path');
const createCompiler = require('./createCompiler').createCompiler;
const getAssets = require('./getAssets');
const extractErrors = require('./extractWebpackErrors');

function watch(options, errorCallback, compilationCallback) {

  options.bootSsrModuleDir = path.join(__dirname, '..');

  // TODO: hot reloading
  const compiler = createCompiler(options);
  const watchOptions = {
    aggregateTimeout: 300
  };
  compiler.watch(watchOptions, (err, stats) => {

    if (err) {
      errorCallback(err);

    } else {

      compilationCallback(
        extractErrors(stats.compilation.errors),
        stats.compilation.warnings,
        getAssets(stats.compilation),
        stats.endTime - stats.startTime
      );
    }
  });

  // OMG HAX. This wakes up the event loop so we don't get stuck
  // on the java side and get a chance to kill the process properly
  setInterval(() => { }, 1000)
}

module.exports = watch;
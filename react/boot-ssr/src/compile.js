'use strict';

const path = require('path');
const createCompiler = require('./createCompiler');
const getAssets = require('./getAssets');

function compile(options, errorCallback, compilationCallback) {

  const bootSsrModuleDir = path.join(__dirname, '..');

  const compiler = createCompiler(bootSsrModuleDir, options);

  compiler.run((err, stats) => {

    if (err) {
      errorCallback(err.message);

    } else {

      compilationCallback(
        stats.compilation.errors,
        stats.compilation.warnings,
        getAssets(stats.compilation),
        stats.endTime - stats.startTime
      );
    }
  });
}

module.exports = compile;
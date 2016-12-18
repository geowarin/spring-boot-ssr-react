'use strict';

const path = require('path');
const createCompiler = require('./createCompiler');
const getAssets = require('./getAssets');

function compile(pages, errorCallback, compilationCallback) {

  const rootDir = path.join(__dirname, '..');

  const compiler = createCompiler(rootDir, pages);

  compiler.run((err, stats) => {

    if (err) {
      errorCallback(err.message);

    } else {

      compilationCallback(
        stats.compilation.errors,
        stats.compilation.warnings,
        getAssets(stats.compilation)
      );
    }
  });
}

module.exports = compile;
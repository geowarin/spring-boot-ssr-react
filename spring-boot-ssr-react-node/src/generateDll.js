'use strict';

const path = require('path');
const createCompiler = require('./createCompiler').createDllCompiler;
const getAssets = require('./getAssets');
const extractErrors = require('./extractWebpackErrors');

function compile(options, errorCallback, compilationCallback) {

  options.bootSsrModuleDir = path.join(__dirname, '..');
  const compiler = createCompiler(options);

  compiler.run((err, stats) => {

    if (err) {
      errorCallback(err);

    } else {

      // LibManifestPlugin writes directly to the output file system for some reason...
      let name = "dll/vendors.manifest.json";
      let manifestPath = path.join(options.projectDirectory, name);
      let source = compiler.outputFileSystem.readFileSync(manifestPath).toString();

      const manifestAsset = {
        name: "vendors.manifest.json",
        source: source
      };

      compilationCallback(
        extractErrors(stats.compilation.errors),
        stats.compilation.warnings,
        getAssets(stats.compilation).concat(manifestAsset),
        stats.endTime - stats.startTime
      );
    }
  });
}

module.exports = compile;
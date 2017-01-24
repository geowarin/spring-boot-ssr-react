'use strict';

const path = require('path');
const createCompiler = require('./createCompiler').createDllCompiler;
const getAssets = require('./getAssets');

function compile(options, errorCallback, compilationCallback) {

  const bootSsrModuleDir = path.join(__dirname, '..');

  const compiler = createCompiler(bootSsrModuleDir, options);

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
        stats.compilation.errors,
        stats.compilation.warnings,
        getAssets(stats.compilation).concat(manifestAsset),
        stats.endTime - stats.startTime
      );
    }
  });
}

module.exports = compile;
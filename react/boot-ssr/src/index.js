'use strict';

const path = require('path');

const glob = require('glob');
const webpack = require('webpack');
const MemoryFileSystem = require('memory-fs');

const config = (entries, rootDir) => ({
  // devtool: 'cheap-module-source-map',
  entry: entries,
  output: {
    path: path.join(__dirname, 'dist'),
    filename: '[name].js'
  },
  module: {
    loaders: [
      {
        test: /\.jsx?$/,
        loader: 'babel-loader',
        exclude: /node_modules/,
        query: {
          // sourceMaps: dev ? 'both' : false,
          presets: [
            require.resolve('babel-preset-latest'),
            require.resolve('babel-preset-react')
          ],
          plugins: [
            require.resolve('babel-plugin-transform-object-rest-spread'),
            [
              require.resolve('babel-plugin-module-resolver'),
              {
                alias: {
                  react: require.resolve('react'),
                  "react-dom": require.resolve('react-dom'),
                  "react-dom/server": require.resolve('react-dom/server'),
                  "axios": require.resolve('axios'),
                  "little-loader": require.resolve('little-loader')
                }
              }
            ]
          ]
        }
      }
    ]
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin({
      name: 'common'
    })
  ],
  resolveLoader: {
    modules: [
      path.join(rootDir, 'node_modules'),
    ]
  }
});


function getPagesEntry(pagesDir) {
  const pages = glob.sync(pagesDir + '/**/*.js');
  const entries = pages.reduce((entries, pagePath) => {
    let pageName = path.basename(path.relative(pagesDir, pagePath), '.js');
    return Object.assign(entries, {
      [pageName]: pagePath
    })
  }, {});
  return entries;
}

function createCompiler(rootDir, pagesDir, errorCallback) {
  const entries = getPagesEntry(pagesDir);
  entries['client'] = path.join(rootDir, "src/client/client.js");
  entries['renderer'] = path.join(rootDir, "src/server/renderer.js");
  let compiler = null;
  try {
    compiler = webpack(config(entries, rootDir));
  } catch (e) {
    errorCallback(e.message);
    return null;
  }
  compiler.outputFileSystem = new MemoryFileSystem();
  return compiler;
}

function bootSsr(userProjectDir, errorCallback, assetCallback) {

  const rootDir = path.join(__dirname, '..');
  const pagesDir = path.join(userProjectDir, 'pages');

  const compiler = createCompiler(rootDir, pagesDir, errorCallback);

  if (compiler) {
    compiler.run((err, stats) => {

      if (err) {
        errorCallback(err.message);

      } else {

        const errors = stats.compilation.errors;
        if (errors.length > 0) {
          errors.forEach(error => {
            errorCallback(error.message);
          })
        }

        const assets = stats.compilation.assets;
        const assetNames = Object.keys(assets);

        assetNames.forEach(assetName => {
          const asset = assets[assetName];
          const source = asset.source();

          assetCallback(assetName, source);
        });
      }
    });
  }
}

module.exports = bootSsr;
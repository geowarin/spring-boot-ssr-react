'use strict';

const path = require('path');

const glob = require('glob');
const webpack = require('webpack');
const MemoryFileSystem = require('memory-fs');
const SaneWatcherPlugin = require('./watcher/SaneWatcherPlugin');

const config = (entries, rootDir, watchDirectories) => ({
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
    }),
    new SaneWatcherPlugin({watchDirectories: watchDirectories})
  ],
  resolveLoader: {
    modules: [
      path.join(rootDir, 'node_modules'),
    ]
  }
});

function getPagesEntry(pages) {
  const entries = pages.reduce((entries, pagePath) => {
    let pageName = path.basename(pagePath, '.js');
    return Object.assign(entries, {
      [pageName]: pagePath
    })
  }, {});
  return entries;
}

function createCompiler(rootDir, options) {
  const entries = getPagesEntry(options.pages);
  entries['client'] = path.join(rootDir, "src/client/client.js");
  entries['renderer'] = path.join(rootDir, "src/server/renderer.js");
  let compiler = webpack(config(entries, rootDir, options.watchDirectories));
  compiler.outputFileSystem = new MemoryFileSystem();
  return compiler;
}

module.exports = createCompiler;
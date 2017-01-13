'use strict';

const path = require('path');

const glob = require('glob');
const webpack = require('webpack');
const MemoryFileSystem = require('memory-fs');
const SaneWatcherPlugin = require('./watcher/SaneWatcherPlugin');

const config = (entries, rootDir, watchDirectories) => ({
  devtool: 'inline-source-map',
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
  const entries = pages.reduce((entries, page) => {
    return Object.assign(entries, {
      [page.name]: page.file
    })
  }, {});
  return entries;
}

function createCompiler(bootSsrModuleDir, options) {
  const entries = getPagesEntry(options.pages);
  entries['client'] = path.join(bootSsrModuleDir, "src/client/client.js");
  entries['renderer'] = path.join(bootSsrModuleDir, "src/server/renderer.js");
  let compiler = webpack(config(entries, bootSsrModuleDir, options.watchDirectories));
  compiler.outputFileSystem = new MemoryFileSystem();
  return compiler;
}

module.exports = createCompiler;
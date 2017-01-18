'use strict';

const path = require('path');

const glob = require('glob');
const webpack = require('webpack');
const MemoryFileSystem = require('memory-fs');
const SaneWatcherPlugin = require('./watcher/SaneWatcherPlugin');

const core = require('@webpack-blocks/core');
const wp = require('@webpack-blocks/webpack-common');
const babel = require('@webpack-blocks/babel6');
const extractText = require('@webpack-blocks/extract-text2');
const postcss = require('@webpack-blocks/postcss');

function resolveLoaders(modules) {
  return () => ({
    resolveLoader: {
      modules: [
        modules
      ]
    }
  });
}

const babelPresets = [
  require.resolve('babel-preset-latest'),
  require.resolve('babel-preset-react')
];
const babelPlugins = [
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
];

let uglifyJs = function () {
  return new webpack.optimize.UglifyJsPlugin({
    compress: {
      warnings: false
    },
    output: {
      comments: false
    },
    screwIe8: true,
    sourceMap: false
  });
};

const config = (entries, rootDir, options) => core.createConfig(webpack, [
  wp.entryPoint(entries),
  wp.setOutput({
    path: path.join(__dirname, 'dist'),
    filename: '[name].js'
  }),
  postcss([]),
  babel({
    presets: babelPresets,
    plugins: babelPlugins
  }),
  wp.addPlugins([
    new webpack.optimize.CommonsChunkPlugin({name: 'common'}),
    new SaneWatcherPlugin({watchDirectories: options.watchDirectories})
  ]),
  extractText(),
  resolveLoaders(path.join(rootDir, 'node_modules')),
  wp.defineConstants({
    'process.env.NODE_ENV': process.env.NODE_ENV
  }),
  core.env('development', [
    wp.sourceMaps('inline-source-map')
  ]),
  core.env('production', [
    wp.addPlugins([
      uglifyJs()
    ])
  ])
]);


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
  let compiler = webpack(config(entries, bootSsrModuleDir, options));
  compiler.outputFileSystem = new MemoryFileSystem();
  return compiler;
}

module.exports = createCompiler;
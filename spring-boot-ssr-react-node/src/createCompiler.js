'use strict';

const path = require('path');

const glob = require('glob');
const webpack = require('webpack');
const MemoryFileSystem = require('memory-fs');
const SaneWatcherPlugin = require('./watcher/SaneWatcherPlugin');

const core = require('@webpack-blocks/core');
const wp = require('@webpack-blocks/webpack2');
const babel = require('@webpack-blocks/babel6');
const extractText = require('@webpack-blocks/extract-text2');
const postcss = require('@webpack-blocks/postcss');

const resolve = require('resolve');

function resolveLoaders(modules) {
  return () => ({
    resolveLoader: {
      modules: [
        modules
      ]
    }
  });
}

function resolveProj(moduleName, options) {
  try {
    return resolve.sync(moduleName, {basedir: options.projectDirectory})
  } catch (e) {
    return resolve.sync(moduleName, {basedir: options.bootSsrModuleDir})
  }
}

const babelPresets = [
  [require.resolve('babel-preset-latest'), {
    "es2015": {
      "modules": false
    }
  }],
  require.resolve('babel-preset-react')
];
const babelPlugins = (options) => [
  require.resolve('babel-plugin-transform-decorators-legacy'),
  require.resolve('babel-plugin-transform-class-properties'),
  require.resolve('babel-plugin-transform-object-rest-spread'),
  [
    require.resolve('babel-plugin-module-resolver'),
    {
      alias: {
        react: resolveProj('react', options),
        "react-dom": resolveProj('react-dom', options),
        "react-dom/server": resolveProj('react-dom/server', options),
        // "axios": require.resolve('axios'),
        // "little-loader": require.resolve('little-loader')
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

function createConfig(configSetters) {
  return core.createConfig(webpack, [createBaseConfig].concat(configSetters))
}

function createBaseConfig(context) {
  return {
    module: {
      loaders: [
        {
          test: context.fileType('text/css'),
          loaders: ['style-loader', 'css-loader']
        }, {
          test: context.fileType('image'),
          loaders: ['file-loader']
        }, {
          test: context.fileType('application/font'),
          loaders: ['file-loader?name=fonts/[name][hash].[ext]']
        }, {
          test: context.fileType('audio'),
          loaders: ['url-loader']
        }, {
          test: context.fileType('video'),
          loaders: ['url-loader']
        }
      ]
    },

    resolve: {
      extensions: ['.js', '.jsx', '.json']
    }
  }
}

const dllConfig = (vendors, rootDir, options) => createConfig([
  wp.entryPoint(vendors),
  wp.setOutput({
    path: path.join(options.projectDirectory, '/dll'),
    filename: 'vendors.dll.js',
    library: 'vendors_library'
  }),
  wp.addPlugins([
    new webpack.DllPlugin({
      path: path.join(options.projectDirectory, '/dll', 'vendors.manifest.json'),
      name: 'vendors_library'
    })
  ]),
  wp.sourceMaps('inline-source-map'),
  wp.customConfig({
    context: options.projectDirectory,
    resolve: {
      modules: [
        // can resolve modules in linked dir
        './node_modules'
        // path.join(rootDir, 'node_modules'),
      ],
      extensions: ['.js', '.jsx']
    }
  })
]);

function dllPlugin(options) {
  if (options.dllManifestContent) {
    return [
      new webpack.DllReferencePlugin({
        context: options.projectDirectory,
        manifest: JSON.parse(options.dllManifestContent)
      })
    ];
  }
  return [];
}

const config = (entries, rootDir, options) => createConfig([
  wp.entryPoint(entries),
  wp.setOutput({
    path: path.join(__dirname, 'dist'),
    filename: '[name].js'
  }),
  postcss([
    require('postcss-import'),
    require('postcss-url'),
    require('postcss-custom-media'),
    require('postcss-media-minmax'),
    require('postcss-color-function'),
    require('autoprefixer'),
    require('postcss-custom-properties'),
    require('postcss-nested')
  ]),
  babel({
    presets: babelPresets,
    plugins: babelPlugins(options)
  }),
  wp.addPlugins([
    new webpack.optimize.CommonsChunkPlugin({name: 'common'}),
    new SaneWatcherPlugin({watchDirectories: options.watchDirectories})
  ]),
  extractText('[name].[contenthash:8].css'),
  resolveLoaders(path.join(rootDir, 'node_modules')),
  wp.defineConstants({
    'process.env.NODE_ENV': process.env.NODE_ENV
  }),
  wp.customConfig({
    context: options.projectDirectory,
    resolve: {
      // modules: [
      //   './node_modules',
        // can resolve modules in linked dir
        // path.join(rootDir, 'node_modules')
      // ],
      extensions: ['.js', '.jsx']
    }
  }),
  core.env('development', [
    wp.sourceMaps('inline-source-map'),
    wp.addPlugins(dllPlugin(options))
  ]),
  core.env('production', [
    wp.addPlugins([
      // uglifyJs()
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

function getVendors(pkg, options) {
  return Object.keys(pkg.dependencies).concat(options.additionalDllLibs);
}

function createDllCompiler(options) {
  const packageJson = path.join(options.projectDirectory, 'package.json');
  const vendors = getVendors(require(packageJson), options);
  let compiler = webpack(dllConfig(vendors, options.bootSsrModuleDir, options));
  compiler.outputFileSystem = new MemoryFileSystem();
  return compiler;
}

function createCompiler(options) {
  const entries = getPagesEntry(options.pages);
  // entries['react-things'] = ['react', 'react-dom'];
  entries['client'] = path.join(options.bootSsrModuleDir, "src/client/client.js");
  entries['renderer'] = path.join(options.bootSsrModuleDir, "src/server/renderer.js");
  let compiler = webpack(config(entries, options.bootSsrModuleDir, options));
  compiler.outputFileSystem = new MemoryFileSystem();
  return compiler;
}

module.exports = {
  createCompiler: createCompiler,
  createDllCompiler: createDllCompiler
};
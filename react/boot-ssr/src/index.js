const path = require('path');
const glob = require('glob');

const WsPlugin = require('./WsPlugin');
const webpack = require('webpack');
const MemoryFileSystem = require('memory-fs');

const config = (entries, rootDir) => ({
  // devtool: 'cheap-module-source-map',
  entry: entries,
  output: {
    path: path.join(__dirname, 'dist'),
    filename: '[name].js'
  },
  plugins: [
    new WsPlugin()
  ],
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
            require.resolve('babel-plugin-transform-object-rest-spread')
          ]
        }
      }
    ]
  },
  resolveLoader: {
    modules: [
      path.join(rootDir, 'node_modules'),
    ]
  }
});


function bootSsr(userProjectDir) {

  const rootDir = path.join(__dirname, '..');
  let pagesDir = path.join(userProjectDir, 'pages');
  let pages = glob.sync(pagesDir + '/**/*.js');
  let entries = pages.reduce((entries, pagePath) => {
    let pageName = path.basename(path.relative(pagesDir, pagePath), '.js');
    return Object.assign(entries, {
      [pageName]: pagePath
    })
  }, {});

  let compiler = null;
  try {
    compiler = webpack(config(entries, rootDir));
  } catch (e) {
    console.log(e.message);
    process.exit(1);
  }
  compiler.outputFileSystem = new MemoryFileSystem();

  compiler.run((err, stats) => {
    if (err) {
      console.log('toto');
      console.error(err.message);
      process.exit(1);
    }

    console.log(stats.toString({
      children: true,
      chunks: false,
      colors: true,
      modules: false
    }));
  });
  // const watchOptions = {
  //   aggregateTimeout: 300
  // };
  // compiler.watch(watchOptions, (err, stats) => {
  //
  //   if (err) {
  //     console.log('toto');
  //     console.error(err.message);
  //     process.exit(1);
  //   }
  //
  //   console.log('compiled!');
  // });
}

module.exports = bootSsr;
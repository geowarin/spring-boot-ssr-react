const path = require('path');
const webpack = require('webpack');

module.exports = {
  // devtool: 'cheap-module-source-map',
  entry: {
    renderer: './utils/renderComponent.js',
    client: './client/client.js',
    vendors: [
      'react',
      'react-dom'
    ]
  },
  output: {
    path: path.join(__dirname, '../resources/scripts'),
    filename: '[name].js'
  },
  module: {
    loaders: [
      {
        test: /\.jsx?$/,
        loader: 'babel-loader',
        exclude: /node_modules/
      }
    ]
  }
};

const { merge } = require('webpack-merge');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const NodemonPlugin = require('nodemon-webpack-plugin');

const common = require('./webpack.common.js');

module.exports = merge(common, {
  devtool: 'inline-source-map',
  mode: 'development',
  plugins: [
    new CleanWebpackPlugin(),
    new NodemonPlugin(),
  ],
  optimization: {
    emitOnErrors: false,
  },
  watch: true,
});

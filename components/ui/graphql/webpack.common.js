const path = require('path');
const nodeExternals = require('webpack-node-externals');

module.exports = {
  module: {
    rules: [
      {
        exclude: [path.resolve(__dirname, 'node_modules')],
        test: /\.ts$/,
        use: 'ts-loader'
      },
    ]
  },
  entry: {
    app: path.join(__dirname, 'src/main.ts'),
  },
  externals: [
    nodeExternals({}), {
      bufferutil: 'commonjs bufferutil',
      'utf-8-validate': 'commonjs utf-8-validate',
    }],
  output: {
    filename: 'main.js',
    path: path.resolve(__dirname, 'dist')
  },
  resolve: {
    extensions: ['.ts', '.js'],
  },
  target: 'node'
};

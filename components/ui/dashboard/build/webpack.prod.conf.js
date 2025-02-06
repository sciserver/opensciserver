'use strict'
const path = require('path')
const utils = require('./utils')
const webpack = require('webpack')
const config = require('../config')
const merge = require('webpack-merge')
const baseWebpackConfig = require('./webpack.base.conf')
const CopyWebpackPlugin = require('copy-webpack-plugin')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const ExtractTextPlugin = require('extract-text-webpack-plugin')
const OptimizeCSSPlugin = require('optimize-css-assets-webpack-plugin')
const { WebpackWarPlugin } = require('webpack-war-plugin')

const env = process.env.NODE_ENV === 'testing'
  ? require('../config/test.env')
  : config.build.env

const webpackConfig = merge(baseWebpackConfig, {
  module: {
    rules: utils.styleLoaders({
      sourceMap: config.build.productionSourceMap,
      extract: true
    })
  },
  devtool: config.build.productionSourceMap ? '#source-map' : false,
  output: {
    path: config.build.assetsRoot,
    filename: utils.assetsPath('js/[name].[chunkhash].js'),
    chunkFilename: utils.assetsPath('js/[id].[chunkhash].js')
  },
  plugins: [
    // http://vuejs.github.io/vue-loader/en/workflow/production.html
    new webpack.DefinePlugin({
      'process.env': env,
      'LOGIN_PORTAL_URL': config.dev.dashboard.loginportal,
      'DATASETS_URL': config.dev.dashboard.datasets,
      'RACM_URL': config.dev.dashboard.racm,
      'COURSEWARE_URL': config.dev.dashboard.courseware,
      'LOGGING_URL': config.dev.dashboard.loggingAPI,
      'CASJOBS_URL': config.dev.dashboard.casJobs,
      'COMPUTE_URL': config.dev.dashboard.compute,
      'SCIQUERY_URL': config.dev.dashboard.sciquery,
      'SCIDRIVE_URL': config.dev.dashboard.scidrive,
      'SKYSERVER_URL': config.dev.dashboard.skyserver,
      'SKYQUERY_URL': config.dev.dashboard.skyquery,
      'USER_GUIDES': config.dev.dashboard.userguides,
      'API_DOCUMENTATION': config.dev.dashboard.apidocumentation,
      'HELP_DESK': config.dev.dashboard.helpdesk,
      'BUG_REPORT_FORM': config.dev.dashboard.bugreportform,
      'FILE_MANAGEMENT': config.dev.dashboard.filemanagement,
      'CREATE_UV': config.dev.dashboard.createuv,
      'SHARE_UV': config.dev.dashboard.shareuv,
      'UNSHARE_UV': config.dev.dashboard.unshareuv,
      'GROUP_MANAGEMENT': config.dev.dashboard.groupsmanagement,
      'CREATE_GROUP': config.dev.dashboard.creategroup,
      'INVITE_USER': config.dev.dashboard.inviteuser,
      'SHARE_RESOURCE': config.dev.dashboard.shareresource,
      'ACTIVITY_LOG': config.dev.dashboard.activitylog,
      'CHANGE_PASSWORD': config.dev.dashboard.changePassword,
      'ALERT_MESSAGE': config.dev.dashboard.alertMessage,
      'ALERT_TYPE': config.dev.dashboard.alertType,
      'SCISERVER_VERSION': config.dev.dashboard.sciserverVersion,
      'DASHBOARD_CONTEXT_PATH': config.dev.dashboard.contextPath,
      'NAVBAR_COLOR': config.dev.dashboard.navbarColor,
      'FONT_FAMILY': config.dev.dashboard.fontFamily,
      'USE_ICONS_FOR_ACTIVITIES': config.dev.dashboard.useIconsForActivities,
      'SHOW_APPLICATION_APP_ROW': config.dev.dashboard.showApplicationAppRow,
      'APPLICATION_NAME': config.dev.dashboard.applicationName,
      'APPLICATION_TAGLINE': config.dev.dashboard.applicationTagline,
      'APPLICATION_HOME_URL': config.dev.dashboard.applicationHomeUrl,
      'DISPLAY_SCISERVER_LOGIN': config.dev.dashboard.displaySciserverLogin,
      'ONECLICK_NOTEBOOK_PATH': config.dev.dashboard.oneclickNotebookPath,
      'APPTILES': config.dev.dashboard.appTiles,
      'ADDAPPTILES': config.dev.dashboard.addAppTiles,
      'NBCONV_URL': config.dev.dashboard.nbconvUrl,
    }),
    // UglifyJs do not support ES6+, you can also use babel-minify for better treeshaking: https://github.com/babel/minify
    new webpack.optimize.UglifyJsPlugin({
      compress: {
        warnings: false
      },
      sourceMap: true
    }),
    // extract css into its own file
    new ExtractTextPlugin({
      filename: utils.assetsPath('css/[name].[contenthash].css')
    }),
    // Compress extracted CSS. We are using this plugin so that possible
    // duplicated CSS from different components can be deduped.
    new OptimizeCSSPlugin({
      cssProcessorOptions: {
        safe: true
      }
    }),
    // generate dist index.html with correct asset hash for caching.
    // you can customize output by editing /index.html
    // see https://github.com/ampedandwired/html-webpack-plugin
    new HtmlWebpackPlugin({
      filename: process.env.NODE_ENV === 'testing'
        ? 'index.html'
        : config.build.index,
      template: 'index.html',
      inject: true,
      minify: {
        removeComments: true,
        collapseWhitespace: true,
        removeAttributeQuotes: true
        // more options:
        // https://github.com/kangax/html-minifier#options-quick-reference
      },
      // necessary to consistently work with multiple chunks via CommonsChunkPlugin
      chunksSortMode: 'dependency'
    }),
    // keep module.id stable when vender modules does not change
    new webpack.HashedModuleIdsPlugin(),
    // split vendor js into its own file
    new webpack.optimize.CommonsChunkPlugin({
      name: 'vendor',
      minChunks: function (module) {
        // any required modules inside node_modules are extracted to vendor
        return (
          module.resource &&
          /\.js$/.test(module.resource) &&
          module.resource.indexOf(
            path.join(__dirname, '../node_modules')
          ) === 0
        )
      }
    }),
    // extract webpack runtime and module manifest to its own file in order to
    // prevent vendor hash from being updated whenever app bundle is updated
    new webpack.optimize.CommonsChunkPlugin({
      name: 'manifest',
      chunks: ['vendor']
    }),
    // copy custom static assets
    new CopyWebpackPlugin([
      {
        from: path.resolve(__dirname, '../static'),
        to: config.build.assetsSubDirectory,
        ignore: ['.*']
      }
    ]),
  ].concat(
    process.env.NODE_ENV === 'testing' ? [] :
      [new WebpackWarPlugin({
        archiveName: config.dev.dashboard.contextPath.replace(/^"\/(.+)\/"$/, '$1'),
        webInf: './web-inf',
      })]
  )
})

if (config.build.productionGzip) {
  const CompressionWebpackPlugin = require('compression-webpack-plugin')

  webpackConfig.plugins.push(
    new CompressionWebpackPlugin({
      asset: '[path].gz[query]',
      algorithm: 'gzip',
      test: new RegExp(
        '\\.(' +
        config.build.productionGzipExtensions.join('|') +
        ')$'
      ),
      threshold: 10240,
      minRatio: 0.8
    })
  )
}

if (config.build.bundleAnalyzerReport) {
  const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin
  webpackConfig.plugins.push(new BundleAnalyzerPlugin())
}

module.exports = webpackConfig

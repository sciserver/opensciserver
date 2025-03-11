'use strict'
const utils = require('./utils')
const webpack = require('webpack')
const config = require('../config')
const merge = require('webpack-merge')
const baseWebpackConfig = require('./webpack.base.conf')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const FriendlyErrorsPlugin = require('friendly-errors-webpack-plugin')

// add hot-reload related code to entry chunks
Object.keys(baseWebpackConfig.entry).forEach(function (name) {
  baseWebpackConfig.entry[name] = ['./build/dev-client'].concat(baseWebpackConfig.entry[name])
})

module.exports = merge(baseWebpackConfig, {
  module: {
    rules: utils.styleLoaders({ sourceMap: config.dev.cssSourceMap })
  },
  // cheap-module-eval-source-map is faster for development
  devtool: '#cheap-module-eval-source-map',
  plugins: [
    new webpack.DefinePlugin({
      'process.env': config.dev.env,
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
    // https://github.com/glenjamin/webpack-hot-middleware#installation--usage
    new webpack.HotModuleReplacementPlugin(),
    new webpack.NoEmitOnErrorsPlugin(),
    // https://github.com/ampedandwired/html-webpack-plugin
    new HtmlWebpackPlugin({
      filename: 'index.html',
      template: 'index.html',
      inject: true
    }),
    new FriendlyErrorsPlugin()
  ]
})

'use strict'
// Template version: 1.1.3
// see http://vuejs-templates.github.io/webpack for documentation.

const path = require('path')

const defaultDashboardConfig = require('./dashboard-default.env');
// Try to load any overrides for default-dashboard.env
let siteSpecificConfig = {};
try {
  //For dev
  // siteSpecificConfig = require('./dashboard{ignore}.env');
  //For prod
  siteSpecificConfig = require('./dashboard.env');
} catch (e) {
  if (!(e instanceof Error && e.code === 'MODULE_NOT_FOUND')) {
    throw e;
  }
}
const assign = require('lodash/assign')
const dashboardConfig = assign({}, defaultDashboardConfig, siteSpecificConfig);

module.exports = {
  build: {
    env: require('./prod.env'),
    dashboard: dashboardConfig,
    index: path.resolve(__dirname, '../dist/index.html'),
    assetsRoot: path.resolve(__dirname, '../dist'),
    assetsSubDirectory: 'static',
    assetsPublicPath: dashboardConfig.contextPath.replace(/^"(.+)"$/g, '$1'),
    productionSourceMap: true,
    // Gzip off by default as many popular static hosts such as
    // Surge or Netlify already gzip all static assets for you.
    // Before setting to `true`, make sure to:
    // npm install --save-dev compression-webpack-plugin
    productionGzip: false,
    productionGzipExtensions: ['js', 'css'],
    // Run the build command with an extra argument to
    // View the bundle analyzer report after build finishes:
    // `npm run build --report`
    // Set to `true` or `false` to always turn it on or off
    bundleAnalyzerReport: process.env.npm_config_report
  },
  dev: {
    env: require('./dev.env'),
    dashboard: dashboardConfig,
    port: process.env.PORT || 8080,
    autoOpenBrowser: true,
    assetsSubDirectory: 'static',
    assetsPublicPath: '/',
    proxyTable: {},
    // CSS Sourcemaps off by default because relative paths are "buggy"
    // with this option, according to the CSS-Loader README
    // (https://github.com/webpack/css-loader#sourcemaps)
    // In our experience, they generally work as expected,
    // just be aware of this issue when enabling this option.
    cssSourceMap: false
  }
}

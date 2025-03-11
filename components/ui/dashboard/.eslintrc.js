// https://eslint.org/docs/user-guide/configuring

module.exports = {
  root: true,
  parser: 'babel-eslint',
  parserOptions: {
    sourceType: 'module'
  },
  env: {
    browser: true,
  },
  extends: 'airbnb-base',
  // required to lint *.vue files
  plugins: [
    'html',
    'chai-friendly'
  ],
  // check if imports actually resolve
  'settings': {
    'import/resolver': {
      'webpack': {
        'config': 'build/webpack.base.conf.js'
      }
    }
  },
  // add your custom rules here
  'rules': {
    // don't require .vue extension when importing
    'import/extensions': ['error', 'always', {
      'js': 'never',
      'vue': 'never'
    }],
    // allow optionalDependencies
    'import/no-extraneous-dependencies': ['error', {
      'optionalDependencies': ['test/unit/index.js']
    }],
    // allow debugger during development
    'no-debugger': process.env.NODE_ENV === 'production' ? 2 : 0,
    /* we make use of alert and confirm here */
    'no-alert': 'off',
    /* HAL documents use dangling _ */
    'no-underscore-dangle': 'off',
    /* to allow for assignments to state in vuex */
    'no-param-reassign': [2, {
      'props': false,
    }],
    'linebreak-style': 'off',
    'no-underscore-dangle': 'off',
    "indent": "off",
    'max-len': ["warn", { "code": 120 }],
    /* workaround for https://github.com/eslint/eslint/issues/2102 */
    "no-unused-expressions": 0,
    "chai-friendly/no-unused-expressions": 2,
    'no-console': 'off'
  },
  // strings from Webpack's DefinePlugin
  "globals": {
    "LOGIN_PORTAL_URL": "",
    "DATASETS_URL": "",
    "RACM_URL": "",
    "COURSEWARE_URL": "",
    "LOGGING_URL": "",
    "CASJOBS_URL": "",
    "COMPUTE_URL": "",
    "SCIQUERY_URL": "",
    "SCIDRIVE_URL": "",
    "SKYSERVER_URL": "",
    "SKYQUERY_URL": "",
    "USER_GUIDES": "",
    "API_DOCUMENTATION": "",
    "HELP_DESK": "",
    "BUG_REPORT_FORM": "",
    "FILE_MANAGEMENT": "",
    "CREATE_UV": "",
    "SHARE_UV": "",
    "UNSHARE_UV": "",
    "GROUP_MANAGEMENT": "",
    "CREATE_GROUP": "",
    "INVITE_USER": "",
    "SHARE_RESOURCE": "",
    "ACTIVITY_LOG": "",
    "CHANGE_PASSWORD": "",
    "VERSION": "",
    "COMMITHASH": "",
    "BRANCH": "",
    "ALERT_MESSAGE": "",
    "ALERT_TYPE": "",
    "SCISERVER_VERSION": "",
    "DASHBOARD_CONTEXT_PATH": "",
    "NAVBAR_COLOR": "",
    "FONT_FAMILY": "",
    "USE_ICONS_FOR_ACTIVITIES": "",
    "SHOW_APPLICATION_APP_ROW": "",
    "APPLICATION_NAME": "",
    "APPLICATION_TAGLINE": "",
    "APPLICATION_HOME_URL": "",
    'DISPLAY_SCISERVER_LOGIN': "",
    'ONECLICK_NOTEBOOK_PATH': "",
    'APPTILES': "",
    'ADDAPPTILES': "",
    'NBCONV_URL': "",
  }
}

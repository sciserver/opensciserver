const apiCall = require('./apiCall').default;
const $ = require('jquery');
const Resources = require('./resources').default;

export default function (_user, _token) {
  const that = this;
  this.user = _user;
  this.username = _user.username;
  this.token = _token;

  function init(jsonEntities) {
    const entities = $.parseJSON(JSON.stringify(jsonEntities));
    that.config.users = entities.users;
    that.config.groups = entities.groups;
    that.Resources = new Resources(that.config, 'resourcestab');
  }

  const loadConfig = (json) => {
    that.config = $.parseJSON(JSON.stringify(json));
    that.config.token = that.token;
    that.config.username = that.username;
    that.config.user = that.user;
    apiCall(that.config.getAllUsersUrl, that.token, 'GET', '', init, null, null);
  };

  apiCall(`${RACM_URL}/config`, this.token, 'GET', '', loadConfig, null);
}

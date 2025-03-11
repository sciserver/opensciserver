import includes from 'lodash/includes';

function makePathComponent(userVolumeObject, path) {
  let url = '';
  if (userVolumeObject.type === 'uservolumes') {
    url = `${userVolumeObject.rootVolumeObj.name}/${userVolumeObject.owner}/${encodeURIComponent(userVolumeObject.name)}`;
  } else {
    url = `${encodeURIComponent(userVolumeObject.name)}`;
  }
  if (path) {
    url = `${url}${path}`;
  }
  return url;
}

function joinBaseURL(userVolumeObject, urlPart) {
  return `${userVolumeObject.apiEndpoint}${urlPart}${makePathComponent(userVolumeObject, null)}/`;
}

function joinURLWithPath(userVolumeObject, urlPart, path) {
  const url = joinBaseURL(userVolumeObject, urlPart);
  if (path !== '') {
    const splitString = path.split('/').filter(part => part.length > 0).map(encodeURIComponent).join('/');
    const fullPath = `${url}${splitString}`;
    return fullPath;
  }
  return url;
}
function joinURLWithFileName(userVolumeObject, urlPart, path, fileName) {
  const url = joinURLWithPath(userVolumeObject, urlPart, path);
  if (path !== '') {
    const fullPath = `${url}/${encodeURIComponent(fileName)}`;
    return fullPath;
  }
  const fullPath = `${url}${encodeURIComponent(fileName)}`;
  return fullPath;
}
/* currentListOfUserVolumes and currentFiles maps these objects to
    * objects with fields for display purposes. */
function getCurrentUVList(myUserVolumeObjects) {
  return myUserVolumeObjects.map(val => ({
    size: '',
    modified: '',
    sharedVolumes: val.rootVolumeObj.containsSharedVolumes,
    grant: includes(val.allowedActions, 'grant'),
    delete: includes(val.allowedActions, 'delete'),
    read: includes(val.allowedActions, 'read'),
    write: includes(val.allowedActions, 'write'),
    sharedUsers: val.sharedWith,
    url: val.fileserviceObj.apiEndpoint,
    ...val,
  }));
}
function getCurrentDVList(myDataVolumeObjects) {
  return myDataVolumeObjects.map(val => ({
    size: '',
    modified: '',
    grant: includes(val.allowedActions, 'grant'),
    delete: includes(val.allowedActions, 'delete'),
    read: includes(val.allowedActions, 'read'),
    write: includes(val.allowedActions, 'write'),
    sharedUsers: val.sharedWith,
    ...val,
  }));
}
function changeVolumesType(type) {
  let selectedVolumeType = '';
  if (type === 'uservolumes') {
    selectedVolumeType = 'uservolumes';
  } else {
    selectedVolumeType = 'datavolumes';
  }
  return selectedVolumeType;
}
function createDownloadFileLink(url, fileName) {
  const link = document.createElement('a');
  link.setAttribute('download', fileName);
  document.body.appendChild(link);
  link.download = fileName;
  link.href = url;
  return link;
}
function clickLink(link) {
  link.click();
}
function removeChildFromDocBody(link) {
  document.body.removeChild(link);
}
function downloadFile(url, fileName) {
  const link = createDownloadFileLink(url, fileName);
  clickLink(link);
  removeChildFromDocBody(link);
}
export default { joinBaseURL,
  joinURLWithPath,
  joinURLWithFileName,
  getCurrentUVList,
  getCurrentDVList,
  changeVolumesType,
  downloadFile,
  createDownloadFileLink,
  clickLink,
  removeChildFromDocBody,
  makePathComponent,
};

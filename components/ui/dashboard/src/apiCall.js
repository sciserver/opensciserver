import handleError from './error-handler';

const $ = require('jquery');

export default function apiCall(
  url, authToken, method, senddata, successcallback, ignored, headers = {}) {
  const allHeaders = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    'X-Auth-Token': authToken,
    ...headers,
  };

  $.ajax({
    url,
    dataType: 'json',
    data: senddata,
    headers: allHeaders,
    type: method,
    success: successcallback,
    error(XMLHttpRequest, textStatus, errorThrown) {
      handleError(XMLHttpRequest.status, url, XMLHttpRequest.responseJSON, errorThrown, textStatus);
    },
  });
}

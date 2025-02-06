import redirectionToLoginPortal from './redirectionToLoginPortal';

let alreadyClosing = false;

export default function handleError(statusCode, requestURL, jsonResponse, errorThrown, textStatus) {
  if (alreadyClosing) return;
  if (statusCode === 401) {
    alert('You have logged out or your session has expired. Please login again.');
    alreadyClosing = true;
    redirectionToLoginPortal();
  } else {
      const err = jsonResponse ? jsonResponse.error : errorThrown;
      const stat = textStatus ? `: Status: ${textStatus}` : '';
      console.error(`Error when connecting to URL [${requestURL}] : ${err} ${stat}`);
  }
}

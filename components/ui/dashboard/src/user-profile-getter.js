import tokenProvider from '@/token-provider';
import Cookies from 'js-cookie';
import axios from 'axios';

export default function () {
  const tokens = tokenProvider();
  if (tokens.length === 0) return Promise.reject('No authentication found');

  const successCallback = (response) => {
    const token = response.config.extraInfo.token;
    Cookies.set('portalCookie', token, { secure: true });
    return {
      userProfile: response.data,
      token,
    };
  };

  let chainedAttempts = axios.get(`${RACM_URL}/ugm/rest/user`, {
    headers: { 'X-Auth-Token': tokens[0] },
    extraInfo: { token: tokens[0] },
  });

  tokens.slice(1).forEach((token) => {
    chainedAttempts = chainedAttempts.catch(() =>
      axios.get(`${RACM_URL}/ugm/rest/user`, {
        headers: { 'X-Auth-Token': token },
        extraInfo: { token },
      }),
    );
  });

  return chainedAttempts.then(successCallback);
}

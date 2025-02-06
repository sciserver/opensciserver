/** @module token-provider */
import Cookies from 'js-cookie';
import queryString from 'query-string';

/**
 * @return {Array}
 */
export default function () {
  const possibleTokens = [];
  const parsed = queryString.parse(location.search);
  if (parsed.token) {
    possibleTokens.push(parsed.token);
  }
  if (Cookies.get('portalCookie')) {
    possibleTokens.push(Cookies.get('portalCookie'));
  }

  return possibleTokens;
}

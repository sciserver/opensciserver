import removeTokenFromUrl from './remove-token-from-url';

export default function redirectionToLoginPortal() {
  // Remove ?token to avoid having multiple ?token= query parameters
  removeTokenFromUrl();
  const callbackURL = encodeURIComponent(window.location.href);
  window.location.href = `${LOGIN_PORTAL_URL}/login?callbackUrl=${callbackURL}`;
}

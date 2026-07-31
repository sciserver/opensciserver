import Cookies from 'js-cookie';
import { ACCESS_TOKEN_KEY, COMPUTE_ACCESS_TOKEN_KEY, COMPUTE_ACCESS_TOKEN_KEY_2 } from 'src/utils/keys';

const isSecureCookie = typeof window !== 'undefined' && window.location.protocol === 'https:';

const cookieOptions = {
  path: '/',
  sameSite: 'none' as const,
  secure: isSecureCookie
};

export const AuthService = {
  getToken(): string {
    return Cookies.get(ACCESS_TOKEN_KEY) || '';
  },

  getComputeToken(): string {
    return Cookies.get(COMPUTE_ACCESS_TOKEN_KEY) || '';
  },

  isAuthenticated(): boolean {
    return Cookies.get(ACCESS_TOKEN_KEY) != null;
  },

  async login(token: string) {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
    sessionStorage.setItem(COMPUTE_ACCESS_TOKEN_KEY, token);
    sessionStorage.setItem(COMPUTE_ACCESS_TOKEN_KEY_2, token);
    Cookies.set(ACCESS_TOKEN_KEY, token, cookieOptions);
    Cookies.set(COMPUTE_ACCESS_TOKEN_KEY, token, cookieOptions);
    Cookies.set(COMPUTE_ACCESS_TOKEN_KEY_2, token, cookieOptions);
  }

};

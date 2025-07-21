import Cookies from 'js-cookie';
import { ACCESS_TOKEN_KEY, COMPUTE_ACCESS_TOKEN_KEY, COMPUTE_ACCESS_TOKEN_KEY_2 } from 'src/utils/keys';

export const AuthService = {
  getToken(): string {
    return Cookies.get(ACCESS_TOKEN_KEY) || '';
  },

  isAuthenticated(): boolean {
    return Cookies.get(ACCESS_TOKEN_KEY) != null;
  },

  async login(token: string) {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
    sessionStorage.setItem(COMPUTE_ACCESS_TOKEN_KEY, token);
    sessionStorage.setItem(COMPUTE_ACCESS_TOKEN_KEY_2, token);
    Cookies.set(ACCESS_TOKEN_KEY, token);
    Cookies.set(COMPUTE_ACCESS_TOKEN_KEY, token);
    Cookies.set(COMPUTE_ACCESS_TOKEN_KEY_2, token);
  }

};

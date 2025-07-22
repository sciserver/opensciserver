// eslint-disable-next-line eslint-comments/disable-enable-pair
/* eslint-disable @typescript-eslint/no-unused-vars */
import { createContext } from 'react';
import { User } from 'src/models/user';

interface UserContextInterface {
  isAuthenticated: boolean;
  setIsAuthenticated: (isAuth: boolean) => void;
  user?: User;
  setUser: (user: User) => void;
  token: string;
  setToken: (token: string) => void;
}

export const UserContext = createContext<UserContextInterface>({
  isAuthenticated: false,
  setIsAuthenticated: (isAuth) => { },
  user: undefined,
  setUser: (user) => { },
  token: '',
  setToken: (token) => { }

});

interface AppContextInterface {
  menuOption: string;
  setMenuOption: (option: string) => void;
  drawerOpen: boolean;
  setDrawerOpen: (open: boolean) => void;
  showAppBar: boolean;
  setShowAppBar: (show: boolean) => void;
}

export const AppContext = createContext<AppContextInterface>({
  menuOption: 'datasets',
  setMenuOption: (option) => { },
  drawerOpen: true,
  setDrawerOpen: (open: boolean) => { },
  showAppBar: true,
  setShowAppBar: (show: boolean) => { }
});

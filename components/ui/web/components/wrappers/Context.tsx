import { useState, FC, useEffect } from 'react';

// Context
import { UserContext, AppContext } from 'context';
import { AuthService } from 'src/services/AuthService';
import { User } from 'src/graphql/typings';

interface Props {
  children: any
}

export const ContextWrapper: FC<Props> = ({ children }) => {
  // set up context following this: https://stackoverflow.com/questions/41030361/how-to-update-react-context-from-inside-a-child-component
  const [user, setUser] = useState<User>();
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [token, setToken] = useState<string>('');

  const userValue = {
    user,
    setUser,
    isAuthenticated,
    setIsAuthenticated,
    token,
    setToken
  };

  const [menuOption, setMenuOption] = useState<string>('datasets');
  const [drawerOpen, setDrawerOpen] = useState<boolean>(false);
  const [showAppBar, setShowAppBar] = useState<boolean>(true);

  const appValue = { menuOption, setMenuOption, drawerOpen, setDrawerOpen, showAppBar, setShowAppBar };

  useEffect(() => {
    const preloadState = async () => {
      const tokenLS = await AuthService.getToken();
      if (tokenLS) {
        setToken(tokenLS);
      }
    };

    preloadState();
  }, []);

  return (
    <UserContext.Provider value={userValue}>
      <AppContext.Provider value={appValue}>{children}</AppContext.Provider>
    </UserContext.Provider>
  );
};

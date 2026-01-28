import { FC, useContext, useEffect, useState } from 'react';
import router from 'next/router';
import { useLazyQuery } from '@apollo/client';
import styled from 'styled-components';
import {
  Divider,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Icon,
  AppBar,
  Avatar,
  IconButton
} from '@mui/material';
import {
  AutoGraph as AutoGraphIcon,
  Folder as FolderIcon,
  Home as HomeIcon,
  Terminal as TerminalIcon
} from '@mui/icons-material';

import { AppContext, UserContext } from 'context';
import { GET_USER } from 'src/graphql/accounts';

import { DrawerOption } from 'components/common/layout';
import { HideOnScroll } from 'components/common/hideOnScroll';
import { Toolbar } from 'components/common/toolbar';
import { stringAvatar } from 'src/utils/account';

export const drawerOpenWidth = 200;
export const drawerClosedWidth = 60;
export const appBarHeight = 64;

type Props = {
  open: boolean
}

type ComponentProps = {
  window?: () => Window;
}

const Styled = styled.div`
  .MuiAppBar-root {
    height: ${appBarHeight}px;
    img {
      margin-left: 15px;
    }
  }

  .MuiToolbar-root{
    display: flex;
    padding-left: 10px;
  }
  
  .MuiDrawer-paper {
    z-index: 0;
    border: none;
    width: ${(props: Props) => props.open ? drawerOpenWidth : drawerClosedWidth}px;
    background-color: ${({ theme }) => theme.palette.secondary.light};
    box-shadow:  0 1px 2px hsla(0,0%,0%,0.05), 0 1px 4px hsla(0, 0%, 0%, 0.05), 0 2px 8px hsla(0, 0%, 0%, 0.05);
  }  

  .drawer-flex {
    height: 100%;
    display: flex;
    margin-bottom: 20px;
    flex-direction: column;
    justify-content:  space-between;

    .user-info {
      display: flex;
      align-items: center;
      color: ${({ theme }) => theme.palette.icons.text};
      span{
        margin-left: 10px;
      }
    }
  }
  
  .contrast {
    color: ${({ theme }) => theme.palette.icons.drawer};
  }
  .contrast-white {
    color: ${({ theme }) => theme.palette.icons.text};
  }

  .MuiListItemButton-root.selected {
    padding-left: 11px !important;
    border-radius: 15px;
    margin: 0 5px;
    background-color:  ${({ theme }) => theme.palette.primary.light};
    span{
      font-weight: 600;
      color:  ${({ theme }) => theme.palette.secondary.main};
    }
  }
`;


export const DrawerNav: FC = (props: ComponentProps) => {

  const { user, setUser } = useContext(UserContext);
  const { drawerOpen, menuOption, setMenuOption, showAppBar } = useContext(AppContext);

  const [getUser, { data: userData }] = useLazyQuery(GET_USER);

  const [toggleDrawerOpen, setToggleDrawerOpen] = useState<boolean>(false);

  const handleOptionChange = (option: string) => {
    setMenuOption(option);
    router.push(`/${option}`);
  };

  // ON MOUNT: UI config
  useEffect(() => {
    setMenuOption(router.asPath.split('/')[1]);

    if (!user) {
      getUser();
    }
  }, []);

  useEffect(() => {
    if (userData && userData.getUser) {
      setUser(userData.getUser);
    }
  }, [userData]);

  const drawerOptions: DrawerOption[] = [
    {
      name: 'Home',
      value: 'home',
      onClick: () => window.location.href = process.env.NEXT_PUBLIC_DASHBOARD_URL || '',
      icon: <HomeIcon />
    },
    {
      name: 'Datasets',
      value: 'datasets',
      onClick: () => handleOptionChange('datasets'),
      icon: <span className="material-symbols-outlined">database </span>
    },
    {
      name: 'Compute',
      value: 'compute',
      onClick: () => handleOptionChange('compute'),
      icon: <AutoGraphIcon />
    },
    {
      name: 'Jobs',
      value: 'jobs',
      onClick: () => handleOptionChange('jobs'),
      icon: <TerminalIcon />
    },
    {
      name: 'Files',
      value: 'files',
      onClick: () => window.location.href = process.env.NEXT_PUBLIC_FILES_URL || '',
      icon: <FolderIcon />
    }
  ];

  return (
    <Styled {...{ open: drawerOpen }}>
      {showAppBar &&
        <HideOnScroll {...props}>
          <AppBar>
            <Toolbar setToggleDrawerOpen={setToggleDrawerOpen} />
          </AppBar>
        </HideOnScroll>
      }
      <Drawer
        variant="permanent"
        open={drawerOpen}
      >
        <Toolbar isDrawer setToggleDrawerOpen={setToggleDrawerOpen} />
        <Divider />
        <div className="drawer-flex">
          <List>
            {drawerOptions.map(option =>
              <ListItem key={option.value} disablePadding>
                <ListItemButton className={menuOption === option.value ? 'selected' : ''} onClick={option.onClick} >
                  <ListItemIcon className={menuOption === option.value ? '' : 'contrast'}>
                    <Icon>{option.icon}</Icon>
                  </ListItemIcon>
                  <ListItemText className={menuOption === option.value ? '' : 'contrast'} primary={option.name} />
                </ListItemButton>
              </ListItem>
            )}
          </List>
          {/* // To be update with username initial in upcoming PR where user details are fetched */}
          <div className="user-info">
            <IconButton>
              <Avatar {...stringAvatar(user?.userName || 'Unknown User')} />
            </IconButton>
            {drawerOpen &&
              <span>{user?.userName}</span>
            }
          </div>
        </div>
      </Drawer>
    </Styled >
  );
};
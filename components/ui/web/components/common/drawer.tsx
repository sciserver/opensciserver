import { FC, useContext, useEffect } from 'react';
import router from 'next/router';
import Image from 'next/image';
import { useLazyQuery } from '@apollo/client';
import styled from 'styled-components';
import {
  ButtonBase,
  Divider,
  Drawer,
  IconButton,
  Tooltip,
  useMediaQuery,
  useTheme
} from '@mui/material';
import {
  AccountCircleOutlined as AccountCircleOutlinedIcon,
  Folder as FolderIcon,
  Home as HomeIcon,
  Menu as MenuIcon,
  MenuOpen as MenuOpenIcon
} from '@mui/icons-material';

import { AppContext, UserContext } from 'context';
import { GET_USER } from 'src/graphql/accounts';

import { DrawerOption } from 'components/common/layout';

import logo from 'public/sciserver-logo.png';
import logoSmall from 'public/logo-sm.png';
import computeLogo from 'public/SciServer_icons_Compute.svg';
import jobsLogo from 'public/sciserver_jobs.png';

// Navigation rail (Material 3): a collapsed rail that is always visible on the leading edge, and
// an expanded rail that is toggled from the menu button. On compact screens the rail is replaced
// by a modal rail opened from a floating menu button.
export const drawerOpenWidth = 154;
export const drawerClosedWidth = 80;

type StyleProps = {
  open: boolean
}

// Soft UI (neumorphism): the rail and its controls share one surface colour and get their depth
// from a light highlight (top-left) and a dark shade (bottom-right). Raised = outer shadows,
// pressed/selected = the same shadows inset.
const railBackground = '#eef3f8';
const shadeColor = '#c3cfdd';
const highlightColor = '#ffffff';
const raised = (d: number) => `${d}px ${d}px ${d * 2}px ${shadeColor}, -${d}px -${d}px ${d * 2}px ${highlightColor}`;
const pressed = (d: number) => `inset ${d}px ${d}px ${d * 2}px ${shadeColor}, inset -${d}px -${d}px ${d * 2}px ${highlightColor}`;

/**
 * Width the rail takes from the page: 0 on compact screens, where the rail is modal.
 */
export const useRailWidth = () => {
  const theme = useTheme();
  const { drawerOpen } = useContext(AppContext);
  const compact = useMediaQuery(theme.breakpoints.down('sm'));

  return { compact, width: compact ? 0 : drawerOpen ? drawerOpenWidth : drawerClosedWidth };
};

const RailContent = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  color: ${({ theme }) => theme.palette.text.primary};

  .rail-header .MuiIconButton-root,
  .user-info .MuiIconButton-root {
    background-color: ${railBackground};
    box-shadow: ${raised(3)};
    transition: box-shadow 150ms;

    &:active {
      box-shadow: ${pressed(3)};
    }
  }

  .user-info .MuiIconButton-root {
    padding: 4px;
    color: ${({ theme }) => theme.palette.text.secondary};
  }

  .rail-header {
    display: flex;
    flex-direction: column;
    align-items: ${(props: StyleProps) => props.open ? 'flex-start' : 'center'};
    gap: 12px;
    padding: 16px ${(props: StyleProps) => props.open ? 16 : 0}px 12px;

    img {
      height: auto;
      align-self: center;
    }
  }

  .rail-nav {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: ${(props: StyleProps) => props.open ? 'stretch' : 'center'};
    gap: ${(props: StyleProps) => props.open ? 4 : 12}px;
    padding: 8px ${(props: StyleProps) => props.open ? 12 : 0}px;
  }

  .rail-item {
    display: flex;
    flex-direction: ${(props: StyleProps) => props.open ? 'row' : 'column'};
    align-items: center;
    justify-content: ${(props: StyleProps) => props.open ? 'flex-start' : 'center'};
    gap: ${(props: StyleProps) => props.open ? 12 : 4}px;
    color: ${({ theme }) => theme.palette.text.secondary};
    border-radius: 28px;
    /* Expanded: the whole row is the indicator. Collapsed: only the icon pill is. */
    padding: ${(props: StyleProps) => props.open ? '0 12px' : '0'};
    height: ${(props: StyleProps) => props.open ? 56 : 'auto'}px;
    width: ${(props: StyleProps) => props.open ? 'auto' : '100%'};
    font-size: ${(props: StyleProps) => props.open ? 14 : 12}px;
    font-weight: 500;
    line-height: 16px;
    text-align: ${(props: StyleProps) => props.open ? 'left' : 'center'};

    .rail-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: ${(props: StyleProps) => props.open ? 24 : 56}px;
      height: ${(props: StyleProps) => props.open ? 24 : 32}px;
      border-radius: 16px;
      transition: box-shadow 150ms;
    }

    &:hover .rail-icon,
    &.Mui-focusVisible .rail-icon {
      box-shadow: ${raised(3)};
    }

    &.Mui-focusVisible {
      outline: 2px solid ${({ theme }) => theme.palette.primary.main};
      outline-offset: 2px;
    }

    &.selected {
      color: ${({ theme }) => theme.palette.primary.main};
      font-weight: 700;

      .rail-icon {
        box-shadow: ${pressed(3)};
      }
    }

    /* Expanded: the indicator is the whole row, not just the icon */
    ${(props: StyleProps) => props.open && `
      &:hover .rail-icon, &.selected .rail-icon { box-shadow: none; }
      &:hover { box-shadow: ${raised(3)}; }
      &.selected { box-shadow: ${pressed(3)}; }
    `}
  }

  .user-info {
    display: flex;
    align-items: center;
    justify-content: ${(props: StyleProps) => props.open ? 'flex-start' : 'center'};
    gap: 8px;
    padding: 0 ${(props: StyleProps) => props.open ? 12 : 0}px 20px;
    color: ${({ theme }) => theme.palette.text.primary};

    span {
      font-weight: 600;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
`;

const paperSx = (width: number) => ({
  '& .MuiDrawer-paper': {
    width,
    bgcolor: railBackground,
    border: 'none',
    overflowX: 'hidden',
    boxShadow: `6px 0 16px ${shadeColor}`
  }
});

export const DrawerNav: FC = () => {

  const { user, setUser } = useContext(UserContext);
  const { drawerOpen, setDrawerOpen, menuOption, setMenuOption } = useContext(AppContext);
  const { compact } = useRailWidth();

  const [getUser, { data: userData }] = useLazyQuery(GET_USER);

  // The rail is shown expanded when it is modal (compact screens) and open
  const expanded = drawerOpen;

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
      name: 'Files',
      value: 'files',
      onClick: () => window.location.href = process.env.NEXT_PUBLIC_FILES_URL || '',
      icon: <FolderIcon />
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
      icon: <Image src={computeLogo} alt="" width={24} height={24} />
    },
    {
      name: 'Jobs',
      value: 'jobs',
      onClick: () => handleOptionChange('jobs'),
      icon: <Image src={jobsLogo} alt="" width={24} height={24} />
    }
  ];

  const content = (
    <RailContent open={expanded}>
      <div className="rail-header">
        <IconButton
          aria-label={expanded ? 'Collapse navigation' : 'Expand navigation'}
          aria-expanded={expanded}
          onClick={() => setDrawerOpen(!drawerOpen)}
        >
          {expanded ? <MenuOpenIcon /> : <MenuIcon />}
        </IconButton>
        {/* The logo is not a button and does not toggle the rail */}
        {expanded
          ? <Image key="logo-wide" src={logo} alt="SciServer logo" width={110} priority />
          : <Image key="logo-mark" src={logoSmall} alt="SciServer logo" width={40} height={40} priority />
        }
      </div>
      <nav className="rail-nav" aria-label="Main navigation">
        {drawerOptions.map((option, index) => (
          <div key={option.value} style={{ display: 'contents' }}>
            <ButtonBase
              className={`rail-item ${menuOption === option.value ? 'selected' : ''}`}
              aria-current={menuOption === option.value ? 'page' : undefined}
              onClick={() => {
                if (compact) {
                  setDrawerOpen(false);
                }
                option.onClick();
              }}
            >
              <span className="rail-icon">{option.icon}</span>
              <span>{option.name}</span>
            </ButtonBase>
            {index === 1 && <Divider flexItem sx={{ margin: '4px 16px', borderColor: shadeColor }} />}
          </div>
        ))}
      </nav>
      <div className="user-info">
        <Tooltip title={expanded ? '' : user?.userName || 'Unknown User'}>
          <IconButton aria-label="Account">
            <AccountCircleOutlinedIcon fontSize="large" />
          </IconButton>
        </Tooltip>
        {expanded && <span>{user?.userName}</span>}
      </div>
    </RailContent>
  );

  return compact
    ? <>
      <IconButton
        aria-label="Open navigation"
        onClick={() => setDrawerOpen(true)}
        sx={{ position: 'fixed', top: 8, left: 8, zIndex: (theme) => theme.zIndex.drawer, bgcolor: 'background.paper', boxShadow: 2 }}
      >
        <MenuIcon />
      </IconButton>
      <Drawer
        variant="temporary"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        ModalProps={{ keepMounted: true }}
        sx={paperSx(drawerOpenWidth)}
      >
        {content}
      </Drawer>
    </>
    : <Drawer variant="permanent" sx={paperSx(drawerOpen ? drawerOpenWidth : drawerClosedWidth)}>
      {content}
    </Drawer>;
};

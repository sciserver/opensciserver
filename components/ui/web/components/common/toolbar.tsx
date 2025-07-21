import { FC, useContext } from 'react';
import Image from 'next/image';

import { IconButton, Toolbar as ToolBarMUI } from '@mui/material';
import { ChevronLeft as ChevronLeftIcon, Menu as MenuIcon } from '@mui/icons-material';

import { AppContext } from 'context';

import logo from 'public/SciServ_redux_logo.png';

type Props = {
  setToggleDrawerOpen: (toogleDrawerOpen: boolean) => void;
  isDrawer?: boolean;
}

export const Toolbar: FC<Props> = ({ setToggleDrawerOpen, isDrawer = false }) => {
  const { drawerOpen, setDrawerOpen } = useContext(AppContext);

  const handleDrawerOpenByMenuButtonClick = () => {
    setToggleDrawerOpen(!drawerOpen);
    setDrawerOpen(!drawerOpen);
  };

  return (
    <ToolBarMUI>
      <IconButton className={`toggle-button-${drawerOpen ? 'open' : 'close'} contrast-white`} aria-label="toggle-drawer-open" onClick={handleDrawerOpenByMenuButtonClick} >
        {drawerOpen ? <ChevronLeftIcon /> : <MenuIcon />}
      </IconButton>
      {(!isDrawer || drawerOpen) &&
        <Image src={logo} alt="Sciserver Logo" width={90} />
      }
    </ToolBarMUI>
  );
};
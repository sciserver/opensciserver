import { FC, useContext } from 'react';
import Link from 'next/link';
import styled from 'styled-components';
import { Alert } from '@mui/material';

import { AppContext } from 'context';
import { DrawerNav, appBarHeight, drawerClosedWidth, drawerOpenWidth } from 'components/common/drawer';

export type DrawerOption = {
  name: string
  value: string
  onClick: () => void
  icon: JSX.Element
}

type Props = {
  children: any
}

type StyleProps = {
  open: boolean
}

export const netMargin = 50;
export const drawerOpenMargin = drawerOpenWidth + netMargin;
export const drawerClosedMargin = drawerClosedWidth + netMargin;

const Styled = styled.div`
  main {
    margin-left: ${(props: StyleProps) => props.open ? drawerOpenMargin : drawerClosedMargin}px;
  }
    
  .alert {
    margin-left: ${(props: StyleProps) => props.open ? drawerOpenWidth : drawerClosedWidth}px;
    margin-top: ${appBarHeight}px;
  }
`;

export const Layout: FC<Props> = ({ children }) => {

  const { drawerOpen } = useContext(AppContext);

  return (
    <Styled {...{ open: drawerOpen }}>
      <DrawerNav />
      <Alert className="alert" severity="warning">
        This is a beta release of SciServer V3.0. We're actively improving it and adding new features.
        If you have any feedback, please let us know by adding an issue to our
        <Link rel="noopener noreferrer" target="_blank" href="https://github.com/sciserver/opensciserver/issues">Github Repo</Link>
        or contacting us at <Link href={`mailto:${process.env.NEXT_PUBLIC_HELPDESK_EMAIL}`}>{process.env.NEXT_PUBLIC_HELPDESK_EMAIL}</Link>.
      </Alert>
      <main>{children}</main>
    </Styled>
  );
};
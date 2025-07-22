import { FC, useContext } from 'react';
import styled from 'styled-components';

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
    margin-top: ${appBarHeight + 20}px;
  }
`;

export const Layout: FC<Props> = ({ children }) => {

  const { drawerOpen } = useContext(AppContext);

  return (
    <Styled {...{ open: drawerOpen }}>
      <DrawerNav />
      <main>{children}</main>
    </Styled>
  );
};
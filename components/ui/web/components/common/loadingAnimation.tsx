import { FC, useContext } from 'react';
import Image from 'next/image';
import { Backdrop } from '@mui/material';
import styled from 'styled-components';

import { AppContext } from 'context';
import logoGif from 'public/sciserver-logo.gif';
import { drawerClosedWidth, drawerOpenWidth } from 'components/common/drawer';

type Props = {
  backDropIsOpen: boolean
}

type StyledProps = {
  drawerOpen: boolean
}

const StyledBackdrop = styled(Backdrop)`
  z-index: 1000;
  margin-left: ${(props: StyledProps) => props.drawerOpen ? drawerOpenWidth : drawerClosedWidth}px;
`;

export const LoadingAnimation: FC<Props> = ({ backDropIsOpen }) => {

  const { drawerOpen } = useContext(AppContext);

  return (
    <StyledBackdrop {...{ drawerOpen }} open={backDropIsOpen} >
      <div className="loading-div">
        <Image src={logoGif} alt="Sciserver logo gif" width={170} />
        <div className="loading">Loading...</div>
      </div>
    </StyledBackdrop>
  );
};
import { FC } from 'react';
import Image from 'next/image';
import { Backdrop } from '@mui/material';
import styled from 'styled-components';

import logoGif from 'public/sciserver-loading-dark-bg.gif';
import { useRailWidth } from 'components/common/drawer';

type Props = {
  backDropIsOpen: boolean
}

type StyledProps = {
  railWidth: number
}

const StyledBackdrop = styled(Backdrop)`
  z-index: 1000;
  margin-left: ${(props: StyledProps) => props.railWidth}px;
`;

export const LoadingAnimation: FC<Props> = ({ backDropIsOpen }) => {

  const { width: railWidth } = useRailWidth();

  return (
    <StyledBackdrop {...{ railWidth }} open={backDropIsOpen} >
      <div className="loading-div">
        <Image src={logoGif} alt="Sciserver is loading logo gif" />
      </div>
    </StyledBackdrop>
  );
};
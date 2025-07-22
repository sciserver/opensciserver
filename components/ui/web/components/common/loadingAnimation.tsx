import { FC } from 'react';
import Image from 'next/image';
import { Backdrop } from '@mui/material';

import logoGif from 'public/sciserver-logo.gif';

type Props = {
  backDropIsOpen: boolean
}

export const LoadingAnimation: FC<Props> = ({ backDropIsOpen }) => {
  return (
    <Backdrop
      open={backDropIsOpen}
    >
      <div className="loading-div">
        <Image src={logoGif} alt="Sciserver logo gif" width={170} />
        <div className="loading">Loading...</div>
      </div>
    </Backdrop>
  );
};
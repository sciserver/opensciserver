
import { FC } from 'react';
import { Slide, useScrollTrigger } from '@mui/material';


type Props = {
  window?: () => Window;
  children: React.ReactElement
}

export const HideOnScroll: FC<Props> = ({ children, window }) => {

  // Note that you normally won't need to set the window ref as useScrollTrigger
  // will default to window.
  // This is only being set here because the demo is in an iframe.
  const trigger = useScrollTrigger({ target: window ? window() : undefined });

  return (
    <Slide appear={false} direction="down" in={!trigger}>
      {children}
    </Slide>
  );
};
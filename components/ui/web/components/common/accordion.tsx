import { FC } from 'react';
import { Accordion } from '@mui/material';
import styled from 'styled-components';

const StyledAccordionComp = styled(Accordion)`
    box-shadow: 0 4px 2px -2px rgba(0, 0, 0, 0.2); 

    .accordion-header {
      display: flex;
      align-items: center;
      gap: 2rem;
      
      .accordion-title { 
        display: flex;
        width: 120px;
        align-items: center;

        .title {
          text-transform: capitalize;
        }
      }
    }
`;

type Props = {
  open: boolean;
  setOpen: (open: boolean) => void;
  children: any;
};

export const StyledAccordion: FC<Props> = ({ open, setOpen, children }) => {

  return <StyledAccordionComp
    expanded={open}
    onChange={() => setOpen(!open)}
    disableGutters
    sx={{ '&:before': { display: 'none' } }}
  >
    {children}
  </StyledAccordionComp>;
};
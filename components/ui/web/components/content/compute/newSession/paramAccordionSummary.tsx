import { FC } from 'react';
import styled from 'styled-components';
import { AccordionSummary, IconButton, Typography } from '@mui/material';
import { ExpandMore as ExpandMoreIcon, Help as HelpIcon } from '@mui/icons-material';
import { InfoCard } from 'components/common/infoCard';

const StyledAccordionSummary = styled(AccordionSummary)`
    height: 130px; 
`;

type Props = {
  title: string;
  open: boolean;
  choice?: {
    name: string;
    subtitle: string;
  }
  extraText?: string;
};

export const ParamAccordionSummary: FC<Props> = ({ title, open, choice, extraText = '' }) => {
  return <StyledAccordionSummary
    expandIcon={<ExpandMoreIcon />}
    aria-controls="domain-content"
    id="domain-header"
  >
    <div className="accordion-header">
      <div className="accordion-title">
        <Typography className="title" variant="h5">{title}</Typography>
        <IconButton onClick={() => { }} >
          <HelpIcon />
        </IconButton>
      </div>
      {!open &&
        <>
          <InfoCard
            title={choice ? choice.name : `No ${title} chosen`}
            subtitle={choice ? choice.subtitle : 'Click here to add'}
            action={() => { }}
            width={400}
          />
          <>
            {extraText &&
              <Typography className="title" variant="h5">{extraText}</Typography>
            }
          </>
        </>
      }
    </div>
  </StyledAccordionSummary>;
};
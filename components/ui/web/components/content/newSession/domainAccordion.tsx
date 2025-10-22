import { FC, useState } from 'react';
import { AccordionDetails } from '@mui/material';
import styled from 'styled-components';

import { ParamAccordionSummary } from 'components/content/newSession/paramAccordionSummary';
import { Domain } from 'src/graphql/typings';
import { StyledAccordion } from 'components/common/accordion';
import { InfoCard } from 'components/common/infoCard';

const StyledAccordionDetails = styled(AccordionDetails)`
    height: auto; 
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;

    .selected {
      box-shadow: inset 0 0 10px  ${({ theme }) => theme.palette.secondary.light};
    }
`;

type Props = {
  domainList: Domain[],
  domainChoice?: Domain,
  setDomainChoice: (domain: Domain) => void
};

export const DomainAccordionSummary: FC<Props> = ({ domainList, domainChoice, setDomainChoice }) => {

  const [open, setOpen] = useState<boolean>(false);

  const handleOnClickOption = (dom: Domain) => {
    setDomainChoice(dom);
    setOpen(false);
  };

  return <StyledAccordion open={open} setOpen={setOpen}>
    <ParamAccordionSummary
      open={open}
      title="Domain"
      choice={domainChoice ? { name: domainChoice.name, subtitle: domainChoice.description || 'Description for chosen domain' } : undefined}
    />
    <StyledAccordionDetails>
      {domainList.map(dom =>
        <InfoCard
          selected={domainChoice!.id === dom.id}
          title={dom.name}
          subtitle={dom.description}
          action={() => handleOnClickOption(dom)}
        />
      )}
    </StyledAccordionDetails>
  </StyledAccordion>;
};
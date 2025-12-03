import { FC, useState } from 'react';
import { AccordionDetails } from '@mui/material';
import styled from 'styled-components';

<<<<<<<< HEAD:components/ui/web/components/content/newResource/singleChoiceAccordion.tsx
import { ParamAccordionSummary } from 'components/content/newResource/paramAccordionSummary';
========
import { ParamAccordionSummary } from 'components/content/newComputeSession/paramAccordionSummary';
>>>>>>>> 7628e03 (✨ New job screen (#77)):components/ui/web/components/content/newComputeSession/singleChoiceAccordion.tsx
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

export type Choice = {
  id: string;
  name: string;
  description?: string;
}

type Props = {
  title: string;
  choiceList: Choice[],
  choice?: Choice,
  setChoice: (choice: Choice) => void
};

export const SingleChoiceAccordionSummary: FC<Props> = ({ title, choiceList, choice, setChoice }) => {
  const [open, setOpen] = useState<boolean>(false);

  const handleOnClickOption = (ch: Choice) => {
    setChoice(ch);
    setOpen(false);
  };

  return <StyledAccordion open={open} setOpen={setOpen}>
    <ParamAccordionSummary
      open={open}
      title={title}
      choice={choice ? { name: choice.name, subtitle: choice.description || 'No description available' } : undefined}
    />
    <StyledAccordionDetails>
      {choiceList.map(ch =>
        <InfoCard
          selected={choice!.id === ch.id}
          title={ch.name}
          subtitle={ch.description || 'No description available'}
          action={() => handleOnClickOption(ch)}
        />
      )}
    </StyledAccordionDetails>
  </StyledAccordion>;
};